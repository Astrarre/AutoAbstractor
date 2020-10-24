package io.github.f2bb.abstracter.func.abstracting.field;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class InterfaceFieldAbstracter<T> implements FieldAbstracter<T> {
	private final FieldAbstracter<T> constant;
	private final FieldAbstracter<T> getter;
	private final FieldAbstracter<T> setter;

	public InterfaceFieldAbstracter(FieldAbstracter<T> constant,
			FieldAbstracter<T> getter,
			FieldAbstracter<T> setter) {
		this.constant = constant;
		this.getter = getter;
		this.setter = setter;
	}

	@Override
	public void abstractField(T header, Class<?> abstracting, Field field, boolean impl) {
		int access = field.getModifiers();
		if(Modifier.isStatic(access) && Modifier.isFinal(access)) {
			this.constant.abstractField(header, abstracting, field, impl);
		} else {
			if(!Modifier.isFinal(access)) {
				this.setter.abstractField(header, abstracting, field, impl);
			}
			this.getter.abstractField(header, abstracting, field, impl);
		}
	}
}
