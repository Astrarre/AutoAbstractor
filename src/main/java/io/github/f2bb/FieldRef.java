package io.github.f2bb;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * annotates a getter or setter method
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldRef {
	String owner();
	String name();
	String type();
}
