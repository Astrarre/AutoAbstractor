package io.github.f2bb.abstracter.func.abstracting.constructor.java;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.TypeVariable;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import io.github.f2bb.ImplementationHiddenException;
import io.github.f2bb.abstracter.func.abstracting.constructor.ConstructorAbstracter;
import io.github.f2bb.abstracter.impl.JavaUtil;
import io.github.f2bb.abstracter.util.ArrayUtil;

public class JavaInterfaceConstructorAbstracter implements ConstructorAbstracter<TypeSpec.Builder> {
	@Override
	public void abstractConstructor(TypeSpec.Builder header,
			Class<?> cls,
			Constructor<?> ctor,
			boolean impl) {
		MethodSpec.Builder method = MethodSpec.methodBuilder("newInstance");
		method.addModifiers(Modifier.STATIC);
		method.addModifiers(Modifier.PUBLIC);
		TypeVariable<? extends Class<?>>[] vars = cls.getTypeParameters();
		if (vars.length != 0) {
			ParameterizedTypeName name = ParameterizedTypeName.get((ClassName) JavaUtil.toTypeName(cls),
					ArrayUtil.map(cls.getTypeParameters(), JavaUtil::toTypeName, TypeName[]::new));
			method.returns(name);
		} else {
			method.returns(JavaUtil.toTypeName(cls));
		}

		for (TypeVariable<? extends Class<?>> parameter : cls.getTypeParameters()) {
			method.addTypeVariable((TypeVariableName) JavaUtil.toTypeName(parameter));
		}

		for (Parameter parameter : ctor.getParameters()) {
			method.addParameter(JavaUtil.toTypeName(parameter.getType()), parameter.getName());
		}

		method.addStatement("throw $T.create()", ImplementationHiddenException.class);
		header.addMethod(method.build());
	}
}
