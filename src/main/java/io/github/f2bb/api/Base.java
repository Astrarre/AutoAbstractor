package io.github.f2bb.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * the class is a base abstraction, and it's super class should be removed
 */
@Retention (RetentionPolicy.CLASS)
@Target ({
		ElementType.CONSTRUCTOR,
		ElementType.FIELD,
		ElementType.METHOD
})
public @interface Base {}
