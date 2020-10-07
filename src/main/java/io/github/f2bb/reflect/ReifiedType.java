package io.github.f2bb.reflect;

import java.lang.reflect.Type;

public class ReifiedType {
	public final Type type;
	public final Class<?> raw;

	public ReifiedType(Type type, Class<?> raw) {
		this.type = type;
		this.raw = raw;
	}

	public Type getType() {
		return this.type;
	}

	public Class<?> getRaw() {
		return this.raw;
	}
}
