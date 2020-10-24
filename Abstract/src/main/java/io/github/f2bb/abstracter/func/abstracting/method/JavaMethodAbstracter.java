package io.github.f2bb.abstracter.func.abstracting.method;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.TypeVariable;
import java.util.Set;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import io.github.f2bb.ImplementationHiddenException;
import io.github.f2bb.abstracter.func.map.TypeMappingFunction;
import io.github.f2bb.abstracter.impl.JavaUtil;

public class JavaMethodAbstracter implements MethodAbstracter<TypeSpec.Builder> {
	private final boolean iface;

	public JavaMethodAbstracter(boolean iface) {this.iface = iface;}

	@Override
	public void abstractMethod(TypeSpec.Builder header, Class<?> abstracting, Method method, boolean impl) {
		TypeMappingFunction reifier = TypeMappingFunction.reify(abstracting);
		java.lang.reflect.Type returnType = reifier.map(method.getGenericReturnType());

		MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getName());
		for (Parameter parameter : method.getParameters()) {
			builder.addParameter(JavaUtil.toTypeName(reifier.map(parameter.getParameterizedType())),
					parameter.getName());
		}
		builder.returns(JavaUtil.toTypeName(returnType));
		for (TypeVariable<Method> parameter : method.getTypeParameters()) {
			builder.addTypeVariable((TypeVariableName) JavaUtil.toTypeName(reifier.map(parameter)));
		}

		Set<Modifier> mods = JavaUtil.getModifiers(method.getModifiers());
		if (iface) {
			mods.remove(javax.lang.model.element.Modifier.FINAL);
			// if instance method
			if (!mods.contains(javax.lang.model.element.Modifier.STATIC)) {
				mods.add(javax.lang.model.element.Modifier.DEFAULT);
				mods.remove(javax.lang.model.element.Modifier.ABSTRACT);
			}
		}
		builder.addModifiers(mods);
		if (!mods.contains(javax.lang.model.element.Modifier.ABSTRACT)) {
			builder.addStatement("throw $T.create()", ImplementationHiddenException.class);
		}

		header.addMethod(builder.build());
	}
}
