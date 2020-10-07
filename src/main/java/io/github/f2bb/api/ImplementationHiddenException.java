package io.github.f2bb.api;


import org.objectweb.asm.Type;

public class ImplementationHiddenException extends RuntimeException {
	public static final String INTERNAL = Type.getInternalName(ImplementationHiddenException.class);
	/**
	 * f2bb intentionally hides the implementation of the class to prevent you from doing dumb things, and to hide all the errors that would be shown if you weren't in a workspace that included
	 * minecraft
	 */
	public static ImplementationHiddenException create() {
		throw new ImplementationHiddenException("Someone didn't set up their dependencies correctly");
	}

	public ImplementationHiddenException(String message) {
		super(message);
	}
}
