package io.github.f2bb.abstracter.func.abstracting;

import java.lang.reflect.Field;

import io.github.f2bb.abstracter.func.filter.Filters;
import io.github.f2bb.abstracter.func.filter.MemberFilter;

public interface FieldAbstracter<T> {
	void abstractField(T header, Class<?> abstracting, Field field);

	default FieldAbstracter<T> ifElse(MemberFilter<Field> filter, FieldAbstracter<T> abstracter) {
		return (h, c, f) -> {
			if (filter.test(c, f)) {
				this.abstractField(h, c, f);
			} else {
				abstracter.abstractField(h, c, f);
			}
		};
	}

	default FieldAbstracter<T> and(FieldAbstracter<T> abstracter) {
		return (h, c, f) -> {
			this.abstractField(h, c, f);
			abstracter.abstractField(h, c, f);
		};
	}

	default FieldAbstracter<T> onlyIf(MemberFilter<Field> filter) {
		return (h, c, f) -> {
			if (filter.test(c, f)) {
				this.abstractField(h, c, f);
			}
		};
	}

	static <T> FieldAbstracter<T> defaultBase(FieldAbstracter<T> empty,
			FieldAbstracter<T> getter,
			FieldAbstracter<T> setter) {
		return getter.and(setter.onlyIf(MemberFilter.<Field>withAccess(Filters.FINAL).negate()))
				       .ifElse(MemberFilter.MINECRAFT_TYPE.or(MemberFilter.withAccess(Filters.STATIC)), empty);
	}
}
