package io.github.f2bb;

public class ImplementationHiddenException extends IllegalStateException {

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
