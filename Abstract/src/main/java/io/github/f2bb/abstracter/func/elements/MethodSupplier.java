package io.github.f2bb.abstracter.func.elements;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.collect.Collections2;
import io.github.f2bb.abstracter.func.filter.MemberFilter;
import io.github.f2bb.abstracter.util.AbstracterLoader;
import io.github.f2bb.abstracter.func.filter.Filters;

public interface MethodSupplier {
	MethodSupplier EMPTY = c -> Collections.emptySet();

	MethodSupplier BASE_DEFAULT = create(AbstracterLoader::isMinecraft)
			                              // neither bridge nor synthetic
			                              .filtered(MemberFilter.<Method>userDeclared()
					                                        // and must be protected
					                                        .and(MemberFilter.<Method>withAccess(Filters.PROTECTED)
							                                             // or public but not static
							                                             .or(MemberFilter.<Method>withAccess(Filters.PUBLIC)
									                                                 .and(MemberFilter.<Method>withAccess(
											                                                 Filters.STATIC).negate())))
					                                        .and(MemberFilter.VALID_PARAMS_AND_RETURN));

	/**
	 * all public methods are exposed from classes inside the 'umbrella'
	 */
	MethodSupplier INTERFACE_DEFAULT = create(AbstracterLoader::isUnabstractedClass)
			                                   .filtered(MemberFilter.<Method>userDeclared().and(MemberFilter.withAccess(
					                                   Filters.PUBLIC))
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

		// inverse virtual order, interface -> super -> this
		for (Class<?> iface : cls.getInterfaces()) {
			if (filter.test(iface)) {
				walk(filter, iface, map);
			}
		}

		Class<?> sup = cls.getSuperclass();
		if (filter.test(sup)) {
			walk(filter, sup, map);
		}
		for (Method method : cls.getDeclaredMethods()) {
			String desc = org.objectweb.asm.Type.getMethodDescriptor(method);
			map.put(method.getName() + ";" + desc, method);
		}
	}
}
