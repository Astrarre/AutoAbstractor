package io.github.f2bb.abstracter.func.inheritance;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.collect.Collections2;
import com.google.common.reflect.TypeToken;
import io.github.f2bb.abstracter.AbstracterConfig;
import io.github.f2bb.abstracter.func.filter.Filters;
import io.github.f2bb.abstracter.func.map.TypeMappingFunction;
import io.github.f2bb.abstracter.util.AbstracterLoader;

// automatically reified
public interface InterfaceFunction {
	InterfaceFunction EMPTY = c -> Collections.emptySet();

	InterfaceFunction SIMPLE = c -> Arrays.asList(c.getGenericInterfaces());
	// todo add `this` class
	InterfaceFunction BASE_DEFAULT = branching(AbstracterLoader::isMinecraft).filtered(Filters.IS_ABSTRACTED);
	InterfaceFunction INTERFACE_DEFAULT =
			branching(AbstracterLoader::isUnabstractedClass).filtered(Filters.IS_ABSTRACTED)
			                                                .add(c -> {
		                                                                                    if (AbstracterConfig
				                                                                                        .isInterfaceAbstracted(
						                                                                                        c.getSuperclass())) {
			                                                                                    return c.getGenericSuperclass();
		                                                                                    } else {
			                                                                                    return null;
		                                                                                    }
	                                                                                    });

	Collection<Type> getInterfaces(Class<?> cls);

	static InterfaceFunction branching(Predicate<Class<?>> superFilter) {
		return (BranchingInterfaceFunction) superFilter::test;
	}

	default InterfaceFunction mapped(TypeMappingFunction function) {
		return c -> Collections2.transform(this.getInterfaces(c), function::map);
	}

	default InterfaceFunction filtered(Predicate<Type> predicate) {
		return c -> Collections2.filter(this.getInterfaces(c), predicate::test);
	}

	default InterfaceFunction add(Function<Class<?>, Type> type) {
		return c -> {
			List<Type> types = new ArrayList<>();
			Type t = type.apply(c);
			if (t != null) {
				types.add(t);
			}
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
