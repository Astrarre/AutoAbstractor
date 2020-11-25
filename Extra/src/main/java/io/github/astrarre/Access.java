package io.github.astrarre;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Access {
	/**
	 * the access flag of this extension method, if this annotation is not present STATIC will be removed!
	 */
	int value() default Modifier.PUBLIC;
}
