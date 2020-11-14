package io.github.f2bb.abstracter.func.elements;

import static io.github.f2bb.abstracter.func.filter.MemberFilter.ACCESSIBLE;
import static io.github.f2bb.abstracter.func.filter.MemberFilter.VALID_PARAMETERS;
import static io.github.f2bb.abstracter.func.filter.MemberFilter.VISIBLE;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.Collections2;
import io.github.f2bb.abstracter.func.filter.MemberFilter;

@SuppressWarnings ({
		"unchecked",
		"rawtypes"
})
public interface ConstructorSupplier {
	ConstructorSupplier EMPTY = c -> Collections.emptySet();
	ConstructorSupplier INTERFACE_DEFAULT = ((ConstructorSupplier) cls -> Arrays.asList(cls.getConstructors()))
			                                        .filter(VALID_PARAMETERS.and((MemberFilter) ACCESSIBLE));

	ConstructorSupplier BASE_DEFAULT = ((ConstructorSupplier) cls -> Arrays.asList(cls.getDeclaredConstructors()))
			                                   .filter(VALID_PARAMETERS.and((MemberFilter) VISIBLE));

	default ConstructorSupplier filter(MemberFilter<Executable> filter) {
		return c -> Collections2.filter(this.getConstructors(c), ctor -> filter.test(c, ctor));
	}

	Collection<Constructor<?>> getConstructors(Class<?> cls);

}
