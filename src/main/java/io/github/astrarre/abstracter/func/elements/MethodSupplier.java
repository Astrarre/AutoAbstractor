package io.github.astrarre.abstracter.func.elements;

import static io.github.astrarre.abstracter.func.filter.MemberFilter.ACCESSIBLE;
import static io.github.astrarre.abstracter.func.filter.MemberFilter.PROTECTED;
import static io.github.astrarre.abstracter.func.filter.MemberFilter.PUBLIC;
import static io.github.astrarre.abstracter.func.filter.MemberFilter.STATIC;
import static io.github.astrarre.abstracter.func.filter.MemberFilter.VALID_PARAMS_VARS_AND_RETURN;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import com.google.common.collect.Collections2;
import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.func.filter.MemberFilter;

@SuppressWarnings ({
		"unchecked",
		"rawtypes"
})
public interface MethodSupplier {
	MethodSupplier EMPTY = (config, c) -> Collections.emptySet();
	// todo check formal parameters
	// todo this needs upgrading to respect filtered methods, if a method is left unexposed
	MethodSupplier BASE_DEFAULT = create(AbstracterConfig::isMinecraft)
			                              // neither bridge nor synthetic
			                              .filtered(VALID_PARAMS_VARS_AND_RETURN.and((MemberFilter) (PUBLIC.and(STATIC.negate())).or(PROTECTED)));

	/**
	 * all public methods are exposed from classes inside the 'umbrella'
	 */
	MethodSupplier INTERFACE_DEFAULT =
			create(AbstracterConfig::isUnabstractedClass).filtered(VALID_PARAMS_VARS_AND_RETURN.and((MemberFilter) ACCESSIBLE));

	static MethodSupplier create(BiPredicate<AbstracterConfig, Class<?>> filter) {
		return (config, c) -> {
			Map<String, Method> map = new HashMap<>();
			walk(config, filter, c, map);
			return map.values();
		};
	}

	/**
	 * @deprecated internal
	 */
	@Deprecated
	static void walk(AbstracterConfig config, BiPredicate<AbstracterConfig, Class<?>> filter, Class<?> cls, Map<String, Method> map) {

		// inverse virtual order, interface -> super -> this
		for (Class<?> iface : cls.getInterfaces()) {
			if (filter.test(config, iface)) {
				walk(config, filter, iface, map);
			}
		}

		Class<?> sup = cls.getSuperclass();
		if (filter.test(config, sup)) {
			walk(config, filter, sup, map);
		}

		for (Method method : cls.getDeclaredMethods()) {
			String desc = org.objectweb.asm.Type.getMethodDescriptor(method);
			map.put(method.getName() + ";" + desc, method);
		}
	}

	default MethodSupplier filtered(MemberFilter<Method> filter) {
		return (config, c) -> Collections2.filter(this.getMethods(config, c), m -> filter.test(config, c, m));
	}

	Collection<Method> getMethods(AbstracterConfig config, Class<?> cls);
}
