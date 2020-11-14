package io.github.f2bb.abstracter.func.elements;

import static io.github.f2bb.abstracter.func.filter.MemberFilter.ACCESSIBLE;
import static io.github.f2bb.abstracter.func.filter.MemberFilter.PROTECTED;
import static io.github.f2bb.abstracter.func.filter.MemberFilter.PUBLIC;
import static io.github.f2bb.abstracter.func.filter.MemberFilter.STATIC;
import static io.github.f2bb.abstracter.func.filter.MemberFilter.VALID_PARAMS_AND_RETURN;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.collect.Collections2;
import io.github.f2bb.abstracter.func.filter.MemberFilter;
import io.github.f2bb.abstracter.util.AbstracterLoader;

@SuppressWarnings ({
		"unchecked",
		"rawtypes"
})
public interface MethodSupplier {
	MethodSupplier EMPTY = c -> Collections.emptySet();
	MethodSupplier BASE_DEFAULT = create(AbstracterLoader::isMinecraft)
			                              // neither bridge nor synthetic
			                              .filtered(VALID_PARAMS_AND_RETURN
					                                        .and((MemberFilter) (PUBLIC.and(STATIC.negate()))
							                                                            .or(PROTECTED)));

	/**
	 * all public methods are exposed from classes inside the 'umbrella'
	 */
	MethodSupplier INTERFACE_DEFAULT = create(AbstracterLoader::isUnabstractedClass)
			                                   .filtered(VALID_PARAMS_AND_RETURN.and((MemberFilter) ACCESSIBLE));

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

	default MethodSupplier filtered(MemberFilter<Method> filter) {
		return c -> Collections2.filter(this.getMethods(c), m -> filter.test(c, m));
	}

	Collection<Method> getMethods(Class<?> cls);
}
