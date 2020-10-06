package io.github.f2bb.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * an element of a class that should only exist in the impl jar, this is used for bridge methods in base abstractions
 */
@Retention (RetentionPolicy.CLASS)
@Target ({
		ElementType.CONSTRUCTOR,
		ElementType.FIELD,
		ElementType.METHOD
})
public @interface Impl {}
