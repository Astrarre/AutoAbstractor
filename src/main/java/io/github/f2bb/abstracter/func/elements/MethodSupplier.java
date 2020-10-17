package io.github.f2bb.abstracter.func.elements;

import static io.github.f2bb.abstracter.func.filter.Filters.PROTECTED;
import static io.github.f2bb.abstracter.func.filter.Filters.PUBLIC;
import static io.github.f2bb.abstracter.func.filter.Filters.STATIC;
import static io.github.f2bb.abstracter.func.filter.MemberFilter.withAccess;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.collect.Collections2;
import io.github.f2bb.abstracter.Abstracter;
import io.github.f2bb.abstracter.func.filter.MemberFilter;
import io.github.f2bb.abstracter.util.AbstracterUtil;

public interface MethodSupplier {
	MethodSupplier EMPTY = c -> Collections.emptySet();

	MethodSupplier BASE_DEFAULT = create(Abstracter::isMinecraft)
			                              // neither bridge nor synthetic
			                              .filtered(MemberFilter.<Method>userDeclared()
					                                        // and must be protected
					                                        .and(MemberFilter.<Method>withAccess(PROTECTED)
							                                             // or public but not static
							                                             .or(MemberFilter.<Method>withAccess(PUBLIC).and(
									                                             MemberFilter.<Method>withAccess(STATIC)
											                                             .negate())))
					                                        .and(MemberFilter.VALID_PARAMS_AND_RETURN));

	/**
	 * all public methods are exposed from classes inside the 'umbrella'
	 */
	MethodSupplier INTERFACE_DEFAULT =
			create(AbstracterUtil::isUnabstractedClass).filtered(MemberFilter.<Method>userDeclared()
			                                                                                    .and(withAccess(PUBLIC))
			                                                                                    .and(MemberFilter.VALID_PARAMS_AND_RETURN));


	Collection<Method> getMethods(Class<?> cls);

	default MethodSupplier filtered(MemberFilter<Method> filter) {
		return c -> Collections2.filter(this.getMethods(c), m -> filter.test(c, m));
	}

	static MethodSupplier create(Predicate<Class<?>> filter) {
		return c -> {
			Map<String, Method> map = new HashMap<>();
			walk(filter, c, map);
			return map.values();
		};
	}

	/**
	 * @deprecated internal
	 */
	@Deprecated
	static void walk(Predicate<Class<?>> filter, Class<?> cls, Map<String, Method> map) {
		if (filter.test(cls)) {
			// inverse virtual order, interface -> super -> this
			for (Class<?> iface : cls.getInterfaces()) {
				walk(filter, iface, map);
			}

			walk(filter, cls.getSuperclass(), map);
			for (Method method : cls.getDeclaredMethods()) {
				String desc = org.objectweb.asm.Type.getMethodDescriptor(method);
				map.put(desc, method);
			}
		}
	}
}
