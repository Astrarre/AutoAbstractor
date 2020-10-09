package io.github.f2bb.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import io.github.f2bb.api.ImplementationHiddenException;

public class JavaUtil {
	public static MethodSpec.Builder generateEmpty(String name, Parameter[] parameters, Type[] params, Type ret, Annotation[] annotations) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder(name);
		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			String s = parameter.getName();
			builder.addParameter(params[i], s);
		}
		builder.returns(ret);
		builder.addStatement("throw $T.create();", ImplementationHiddenException.class);
		for (Annotation annotation : annotations) {
			builder.addAnnotation(AnnotationSpec.get(annotation));
		}
		return builder;
	}

	public static MethodSpec.Builder generateEmpty(String name, String[] parameters, Type[] params, Type ret, Annotation[] annotations) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder(name);
		for (int i = 0; i < parameters.length; i++) {
			builder.addParameter(params[i], parameters[i]);
		}
		builder.returns(ret);
		builder.addStatement("throw $T.create();", ImplementationHiddenException.class);
		for (Annotation annotation : annotations) {
			builder.addAnnotation(AnnotationSpec.get(annotation));
		}
		return builder;
	}

	/**
	 * default not included
	 */
	public static List<Modifier> getModifiers(int modifier) {
		List<javax.lang.model.element.Modifier> modifiers = new ArrayList<>();
		if(java.lang.reflect.Modifier.isAbstract(modifier)) modifiers.add(javax.lang.model.element.Modifier.ABSTRACT);
		if(java.lang.reflect.Modifier.isTransient(modifier)) modifiers.add(javax.lang.model.element.Modifier.TRANSIENT);
		if(java.lang.reflect.Modifier.isFinal(modifier)) modifiers.add(javax.lang.model.element.Modifier.FINAL);
		if(java.lang.reflect.Modifier.isNative(modifier)) modifiers.add(javax.lang.model.element.Modifier.NATIVE);
		if(java.lang.reflect.Modifier.isPrivate(modifier)) modifiers.add(javax.lang.model.element.Modifier.PRIVATE);
		if(java.lang.reflect.Modifier.isProtected(modifier)) modifiers.add(javax.lang.model.element.Modifier.PROTECTED);
		if(java.lang.reflect.Modifier.isPublic(modifier)) modifiers.add(javax.lang.model.element.Modifier.PUBLIC);
		if(java.lang.reflect.Modifier.isStatic(modifier)) modifiers.add(javax.lang.model.element.Modifier.STATIC);
		if(java.lang.reflect.Modifier.isStrict(modifier)) modifiers.add(javax.lang.model.element.Modifier.STRICTFP);
		if(java.lang.reflect.Modifier.isSynchronized(modifier)) modifiers.add(javax.lang.model.element.Modifier.SYNCHRONIZED);
		if(java.lang.reflect.Modifier.isVolatile(modifier)) modifiers.add(javax.lang.model.element.Modifier.VOLATILE);
		return modifiers;
	}
}
