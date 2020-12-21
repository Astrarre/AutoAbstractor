package io.github.astrarre.abstracter.ex;

public class InvalidClassException extends IllegalArgumentException {
	public InvalidClassException(Class<?> cls) {
		super(cls.toGenericString());
	}
}
