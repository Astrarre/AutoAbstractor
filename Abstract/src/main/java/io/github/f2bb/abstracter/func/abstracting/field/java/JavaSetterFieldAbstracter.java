package io.github.f2bb.abstracter.func.abstracting.field.java;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Set;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.github.f2bb.DoNotOverride;
import io.github.f2bb.ImplementationHiddenException;
import io.github.f2bb.abstracter.func.abstracting.field.FieldAbstracter;
import io.github.f2bb.abstracter.func.abstracting.field.FieldAbstraction;
import io.github.f2bb.abstracter.func.map.TypeMappingFunction;
import io.github.f2bb.abstracter.impl.JavaUtil;

public class JavaSetterFieldAbstracter implements FieldAbstracter<TypeSpec.Builder> {
	private final boolean iface;

	public JavaSetterFieldAbstracter(boolean iface) {this.iface = iface;}

	@Override
	public void abstractField(TypeSpec.Builder header, Class<?> abstracting, Field field, boolean impl) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder(FieldAbstraction.getEtterName("set",
				field.getType(),
				field.getName()));
		Set<Modifier> modifiers = JavaUtil.getModifiers(field.getModifiers());
		if (this.iface) {
			modifiers.remove(javax.lang.model.element.Modifier.FINAL);
			modifiers.add(javax.lang.model.element.Modifier.DEFAULT);
		} else {
			modifiers.add(javax.lang.model.element.Modifier.FINAL);
		}
		builder.addModifiers(modifiers);

		builder.addParameter(JavaUtil.toTypeName(TypeMappingFunction.reify(abstracting, field.getGenericType())), field.getName());
		builder.addStatement("throw $T.create()", ImplementationHiddenException.class);
		builder.addAnnotation(AnnotationSpec.builder(DoNotOverride.class).build());
		for (Annotation annotation : field.getDeclaredAnnotations()) {
			builder.addAnnotation(AnnotationSpec.get(annotation));
		}
		header.addMethod(builder.build());
	}
}
