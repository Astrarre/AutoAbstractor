package io.github.f2bb.abstracter.func.inheritance;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.google.common.collect.Collections2;
import com.google.common.reflect.TypeToken;
import io.github.f2bb.abstracter.AbstracterConfig;
import io.github.f2bb.abstracter.func.filter.Filters;
import io.github.f2bb.abstracter.func.map.TypeMappingFunction;
import io.github.f2bb.abstracter.util.AbstracterLoader;

/**
 * This finds the interfaces to expose for the class
 */
public interface InterfaceFunction {
	/**
	 * No interfaces, base classes add themselves as interface abstraction in their post processor
	 */
	InterfaceFunction EMPTY = c -> Collections.emptySet();

	InterfaceFunction SIMPLE = c -> Arrays.asList(c.getGenericInterfaces());

	/**
	 * if a base class doesn't have an interface abstraction, and since they can't extend each other we need to find all
	 * of the interfaces in the hierarchy
	 */
	InterfaceFunction BASE_NO_INTERFACE = branching(AbstracterLoader::isMinecraft).filtered(Filters.IS_VALID);


	InterfaceFunction INTERFACE_DEFAULT = branching(AbstracterLoader::isUnabstractedClass)
			                                      .filtered(Filters.IS_VALID)
			                                      .and(c -> Optional.of(c)
			                                                        .filter(c2 -> AbstracterConfig.isInterfaceAbstracted(c2.getSuperclass()))
			                                                        .map(Class::getGenericSuperclass)
			                                                        .map(Collections::singleton)
			                                                        .orElseGet(Collections::emptySet));

	static InterfaceFunction branching(Predicate<Class<?>> superFilter) {
		return (BranchingInterfaceFunction) superFilter::test;
	}

	default InterfaceFunction mapped(TypeMappingFunction function) {
		return c -> Collections2.transform(this.getInterfaces(c), function::map);
	}

	Collection<Type> getInterfaces(Class<?> cls);

	default InterfaceFunction filtered(Predicate<Type> predicate) {
		return c -> Collections2.filter(this.getInterfaces(c), predicate::test);
	}

	default InterfaceFunction and(InterfaceFunction function) {
		return c -> {
			Collection<Type> types = this.getInterfaces(c);
			types.addAll(function.getInterfaces(c));
			return types;
		};
	}

	interface BranchingInterfaceFunction extends InterfaceFunction {
		@Override
		default List<Type> getInterfaces(Class<?> cls) {
			List<Type> interfaces = new ArrayList<>();
			this.visitInterfaces(TypeToken.of(cls), cls, interfaces);
			return interfaces;
		}

		default void visitInterfaces(TypeToken<?> original, Class<?> cls, List<Type> classes) {
			if (cls == null) {
				return;
			}

			for (Class<?> iface : cls.getInterfaces()) {
				if (AbstracterConfig.isInterfaceAbstracted(iface)) {
					classes.add(original.resolveType(iface).getType());
				} else if (AbstracterLoader.isMinecraft(iface)) {
					// if the minecraft class just wasn't abstracted
					// then we need to find all it's interfaces and add them
					this.visitInterfaces(original, iface, classes);
				}
			}
			Class<?> sup = cls.getSuperclass();
			// if the class isn't abstracted, then we need to get all it's interfaces and bring those down
			// for base this should be isMinecraft
			if (this.visitSuper(cls)) {
				this.visitInterfaces(original, sup, classes);
			}
		}

		boolean visitSuper(Class<?> cls);
	}
}
