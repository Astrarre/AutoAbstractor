package io.github.f2bb.abstracter.func.abstracting.field;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import io.github.f2bb.abstracter.func.map.TypeMappingFunction;
import io.github.f2bb.abstracter.util.AbstracterLoader;

public class BaseFieldAbstracter<T> implements FieldAbstracter<T> {
	private final FieldAbstracter<T> virtual, getter, setter;

	public BaseFieldAbstracter(FieldAbstracter<T> virtual, FieldAbstracter<T> getter, FieldAbstracter<T> setter) {
		this.virtual = virtual;
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public void abstractField(T header, Class<?> abstracting, Field field, boolean impl) {
		if (AbstracterLoader.isMinecraft(TypeMappingFunction.raw(abstracting, field.getGenericType()))) {
			if (!impl) {
				this.virtual.abstractField(header, abstracting, field, false);
			}
		} else {
			if (!Modifier.isFinal(field.getModifiers())) {
				this.setter.abstractField(header, abstracting, field, impl);
			}
			this.getter.abstractField(header, abstracting, field, impl);
		}
	}
}
