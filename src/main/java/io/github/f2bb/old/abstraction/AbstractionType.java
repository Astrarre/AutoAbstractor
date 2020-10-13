package io.github.f2bb.old.abstraction;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

import com.google.common.reflect.TypeToken;

public interface AbstractionType {
	/**
	 * @return the parameterized super class type of the generated class
	 */
	TypeToken<?> getSuperClass();

	// todo make a custom type impl that doesn't get remapped, and is implemented in the toSignature and toTypeName
	//  functions

	/**
	 * @return the interfaces of the generated class
	 */
	Collection<TypeToken<?>> getInterfaces();

	/**
	 * @return the list of methods to abstract
	 */
	Collection<Method> getMethods();

	/**
	 * @return the list of fields to abstract
	 */
	Collection<Field> getFields();
}
