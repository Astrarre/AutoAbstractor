package io.github.astrarre.abstracter.func.elements;

import static io.github.astrarre.abstracter.func.filter.MemberFilter.PROTECTED;
import static io.github.astrarre.abstracter.func.filter.MemberFilter.PUBLIC;
import static io.github.astrarre.abstracter.func.filter.MemberFilter.STATIC;
import static io.github.astrarre.abstracter.func.filter.MemberFilter.VALID_TYPE;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import com.google.common.collect.Collections2;
import io.github.astrarre.abstracter.func.filter.MemberFilter;
import io.github.astrarre.abstracter.util.AbstracterLoader;

@SuppressWarnings ("unchecked")
public interface FieldSupplier {
	FieldSupplier EMPTY = c -> Collections.emptySet();

	FieldSupplier CONSTANTS = ((FieldSupplier)c -> Arrays.asList(c.getDeclaredFields())).filtered((MemberFilter)PUBLIC.and(STATIC));

	FieldSupplier INTERFACE_DEFAULT = create(AbstracterLoader::isUnabstractedClass)
			                                  .filtered(VALID_TYPE.and((MemberFilter) PUBLIC));

	FieldSupplier BASE_DEFAULT =
			create(AbstracterLoader::isMinecraft).filtered(VALID_TYPE.and((MemberFilter) (PUBLIC.and(STATIC.negate())).or(PROTECTED)));

	static FieldSupplier create(Predicate<Class<?>> filter) {
		return c -> {
			List<Field> fields = new ArrayList<>();
			Class<?> cls = c;
			do {
				fields.addAll(Arrays.asList(cls.getDeclaredFields()));
				cls = cls.getSuperclass();
			} while (filter.test(cls));
			return fields;
		};
	}

	default FieldSupplier filtered(MemberFilter<Field> filter) {
		return c -> Collections2.filter(this.getFields(c), m -> filter.test(c, m));
	}

	Collection<Field> getFields(Class<?> cls);
}
