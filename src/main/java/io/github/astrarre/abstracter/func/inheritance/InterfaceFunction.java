package io.github.astrarre.abstracter.func.inheritance;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import com.google.common.collect.Collections2;
import com.google.common.reflect.TypeToken;
import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.func.filter.Filters;
import io.github.astrarre.abstracter.func.map.TypeMappingFunction;

/**
 * This finds the interfaces to expose for the class
 */
public interface InterfaceFunction {
	/**
	 * No interfaces, base classes add themselves as interface abstraction in their post processor
	 */
	InterfaceFunction EMPTY = (config, c) -> Collections.emptySet();

	InterfaceFunction SIMPLE = (config, c) -> Arrays.asList(c.getGenericInterfaces());

	/**
	 * if a base class doesn't have an interface abstraction, and since they can't extend each other we need to find all
	 * of the interfaces in the hierarchy
	 */
	InterfaceFunction BASE_NO_INTERFACE = branching(AbstracterConfig::isMinecraft).filtered(Filters.IS_VALID);


	InterfaceFunction INTERFACE_DEFAULT = branching(AbstracterConfig::isUnabstractedClass)
			                                      .filtered(Filters.IS_VALID)
			                                      .and((config, c) -> Optional.of(c)
			                                                                  .filter((c2) -> c2.getSuperclass() != null && config.isInterfaceAbstracted(c2.getSuperclass()))
			                                                                  .map(Class::getGenericSuperclass)
			                                                                  .map(Collections::singleton)
			                                                                  .orElseGet(Collections::emptySet));

	static InterfaceFunction branching(BiPredicate<AbstracterConfig, Class<?>> superFilter) {
		return (BranchingInterfaceFunction) superFilter::test;
	}

	default InterfaceFunction mapped(TypeMappingFunction function) {
		return (config, c) -> Collections2.transform(this.getInterfaces(config, c), function::map);
	}

	Collection<Type> getInterfaces(AbstracterConfig config, Class<?> cls);

	default InterfaceFunction filtered(BiPredicate<AbstracterConfig, Type> predicate) {
		return (config, c) -> {
			Collection<Type> types = this.getInterfaces(config, c);
			types.removeIf(type -> !predicate.test(config, type));
			return types;
		};
	}

	default InterfaceFunction and(InterfaceFunction function) {
		return (config, c) -> {
			Collection<Type> types = this.getInterfaces(config, c);
			types.addAll(function.getInterfaces(config, c));
			return types;
		};
	}

	interface BranchingInterfaceFunction extends InterfaceFunction {
		@Override
		default List<Type> getInterfaces(AbstracterConfig config, Class<?> cls) {
			List<Type> interfaces = new ArrayList<>();
			this.visitInterfaces(config, TypeToken.of(cls), cls, interfaces);
			return interfaces;
		}

		default void visitInterfaces(AbstracterConfig config, TypeToken<?> original, Class<?> cls, List<Type> classes) {
			if (cls == null) {
				return;
			}

			for (Class<?> iface : cls.getInterfaces()) {
				if (config.isInterfaceAbstracted(iface)) {
					classes.add(original.resolveType(iface).getType());
				} else if (config.isMinecraft(iface)) {
					// if the minecraft class just wasn't abstracted
					// then we need to find all it's interfaces and add them
					this.visitInterfaces(config, original, iface, classes);
				}
			}
			Class<?> sup = cls.getSuperclass();
			// if the class isn't abstracted, then we need to get all it's interfaces and bring those down
			// for base this should be isMinecraft
			if (this.visitSuper(config, cls)) {
				this.visitInterfaces(config, original, sup, classes);
			}
		}

		boolean visitSuper(AbstracterConfig config, Class<?> cls);
	}
}
