package io.github.f2bb.ex;

import org.objectweb.asm.Type;

public class ImplementationHiddenException extends IllegalStateException {
	public static final String INTERNAL = Type.getInternalName(ImplementationHiddenException.class);

	public ImplementationHiddenException(String s) {
		super(s);
	}

	public static <T> T instance() {
		throw create();
	}

	public static ImplementationHiddenException create() {
		// todo add a help link
		throw new ImplementationHiddenException("Someone didn't set up their dependencies correctly");
	}
}
