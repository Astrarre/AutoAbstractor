package io.github.f2bb.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

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
}
