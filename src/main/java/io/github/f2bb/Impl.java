package io.github.f2bb;

public class Impl extends IllegalStateException {
	public static final String FIELD_INIT = "init";
	public static final String EXCEPTION = "call";
	public Impl(String s) {
		super(s);
	}

	/**
	 * This method is used to hide the true initializer for a constant field
	 * quite weird innit
	 */
	public static <T> T init() {
		throw call();
	}

	public static Impl call() {
		// todo add a help link
		return new Impl("Someone didn't set up their dependencies correctly");
	}
}
