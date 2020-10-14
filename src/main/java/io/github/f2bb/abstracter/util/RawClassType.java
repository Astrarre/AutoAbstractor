package io.github.f2bb.abstracter.util;

import java.lang.reflect.Type;

// todo add sign?
public class RawClassType implements Type {
	private final org.objectweb.asm.Type type;

	public RawClassType(org.objectweb.asm.Type type) {this.type = type;}

	public String getInternalName() {
		return this.type.getInternalName();
	}
	@Override
	public String toString() {
		return this.type.getDescriptor();
	}
}
