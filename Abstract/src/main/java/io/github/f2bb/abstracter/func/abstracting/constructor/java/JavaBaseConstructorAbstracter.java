package io.github.f2bb.abstracter.func.abstracting.constructor.java;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import io.github.f2bb.abstracter.func.abstracting.constructor.ConstructorAbstracter;
import io.github.f2bb.abstracter.util.java.JavaUtil;

public class JavaBaseConstructorAbstracter implements ConstructorAbstracter<TypeSpec.Builder> {
	@Override
	public void abstractConstructor(TypeSpec.Builder header,
			Class<?> cls,
			Constructor<?> ctor,
			boolean impl) {
		MethodSpec.Builder method = MethodSpec.constructorBuilder();
		method.addModifiers(JavaUtil.getModifiers(ctor.getModifiers()));
		List<String> params = new ArrayList<>();
		for (Parameter parameter : ctor.getParameters()) {
			String name = parameter.getName();
			params.add(name);
			method.addParameter(JavaUtil.toTypeName(parameter.getType()), name);
		}

		for (TypeVariable<? extends Class<?>> parameter : cls.getTypeParameters()) {
			method.addTypeVariable((TypeVariableName) JavaUtil.toTypeName(parameter));
		}

		method.addStatement("super($L)", String.join(",", params));
		header.addMethod(method.build());
	}
}
