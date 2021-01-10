package io.github.astrarre.abstracter.func.elements;

import static io.github.astrarre.abstracter.func.filter.MemberFilter.ACCESSIBLE;
import static io.github.astrarre.abstracter.func.filter.MemberFilter.VALID_PARAMETERS;
import static io.github.astrarre.abstracter.func.filter.MemberFilter.VISIBLE;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.Collections2;
import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.func.filter.MemberFilter;

@SuppressWarnings ({
		"unchecked",
		"rawtypes"
})
public interface ConstructorSupplier {
	ConstructorSupplier EMPTY = (config, c) -> Collections.emptySet();
	ConstructorSupplier INTERFACE_DEFAULT = ((ConstructorSupplier) (config, cls) -> Arrays.asList(cls.getConstructors()))
			                                        .filter(VALID_PARAMETERS.and((MemberFilter) ACCESSIBLE));

	ConstructorSupplier BASE_DEFAULT = ((ConstructorSupplier) (config, cls) -> Arrays.asList(cls.getDeclaredConstructors()))
			                                   .filter(VALID_PARAMETERS.and((MemberFilter) VISIBLE));

	default ConstructorSupplier filter(MemberFilter<Executable> filter) {
		return (config, c) -> Collections2.filter(this.getConstructors(config, c), ctor -> filter.test(config, c, ctor));
	}

	Collection<Constructor<?>> getConstructors(AbstracterConfig config, Class<?> cls);

}
