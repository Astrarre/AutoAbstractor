package io.github.f2bb.ex;

public class InvalidClassException extends IllegalArgumentException {
	public InvalidClassException(Class<?> cls) {
		super(cls.toGenericString());
	}
}
