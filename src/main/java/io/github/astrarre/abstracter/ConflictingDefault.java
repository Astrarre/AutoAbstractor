package io.github.astrarre.abstracter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * this annotation is added to interface delegate methods that have un-mapped signatures and will conflict with a default method in some interface
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface ConflictingDefault {}
