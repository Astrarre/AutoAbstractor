package io.github.f2bb.abstracter.func.elements;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import com.google.common.collect.Collections2;
import io.github.f2bb.abstracter.Abstracter;
import io.github.f2bb.abstracter.func.filter.Filters;
import io.github.f2bb.abstracter.func.filter.MemberFilter;
import io.github.f2bb.abstracter.util.AbstracterUtil;

public interface FieldSupplier {
	FieldSupplier EMPTY = c -> Collections.emptySet();

	FieldSupplier INTERFACE_DEFAULT = create(AbstracterUtil::isUnabstractedClass)
			                                  .filtered(MemberFilter.withType(Filters.IS_ABSTRACTED)
			                                                        .and(MemberFilter.withAccess(Filters.PUBLIC)));
	FieldSupplier BASE_DEFAULT = create(Abstracter::isMinecraft).filtered(MemberFilter.withType(Filters.IS_ABSTRACTED)
	                                                                                  .and(// and must be protected
			                                                                                  MemberFilter.<Field>withAccess(
					                                                                                  Filters.PROTECTED)
					                                                                                  // or public but
					                                                                                  // not
					                                                                                  // static
					                                                                                  .or(MemberFilter.<Field>withAccess(
							                                                                                  Filters.PUBLIC)
							                                                                                      .and(MemberFilter.<Field>withAccess(
									                                                                                      Filters.STATIC)
									                                                                                           .negate()))));

	Collection<Field> getFields(Class<?> cls);

	default FieldSupplier filtered(MemberFilter<Field> filter) {
		return c -> Collections2.filter(this.getFields(c), m -> filter.test(c, m));
	}

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
}
