package io.github.astrarre.abstracter;

import java.lang.reflect.Type;

import com.google.common.reflect.TypeToken;

public abstract class Cls<T> {
	private static final Type T_VAR = Cls.class.getTypeParameters()[0];
	public Class<T> get() {
		TypeToken<?> token = TypeToken.of(this.getClass());
		return (Class<T>) token.resolveType(T_VAR).getRawType();
	}

	public String getClassName() {
		try {
			return this.get().getName();
		} catch (TypeNotPresentException e) {
			return e.typeName();
		}
	}
}
