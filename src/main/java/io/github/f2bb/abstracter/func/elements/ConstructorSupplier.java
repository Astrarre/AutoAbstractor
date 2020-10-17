package io.github.f2bb.abstracter.func.elements;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.Collections2;
import io.github.f2bb.abstracter.func.filter.Filters;
import io.github.f2bb.abstracter.func.filter.MemberFilter;

public interface ConstructorSupplier {
	ConstructorSupplier EMPTY = c -> Collections.emptySet();

	ConstructorSupplier INTERFACE_DEFAULT =
			((ConstructorSupplier) cls -> Arrays.asList(cls.getDeclaredConstructors())).filter(
			MemberFilter.<Constructor<?>>withParameters(Filters.IS_ABSTRACTED).and(MemberFilter.withAccess(Filters.PUBLIC)));
	ConstructorSupplier BASE_DEFAULT =
			((ConstructorSupplier) cls -> Arrays.asList(cls.getDeclaredConstructors())).filter(
			MemberFilter.<Constructor<?>>withParameters(Filters.IS_ABSTRACTED).and(MemberFilter.<Constructor<?>>withAccess(
					Filters.PUBLIC).or(MemberFilter.withAccess(Filters.PROTECTED))));

	Collection<Constructor<?>> getConstructors(Class<?> cls);

	default ConstructorSupplier filter(MemberFilter<Constructor<?>> filter) {
		return c -> Collections2.filter(this.getConstructors(c), ctor -> filter.test(c, ctor));
	}

}
