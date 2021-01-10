package io.github.astrarre.abstracter.abs.field;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import io.github.astrarre.abstracter.abs.AbstractAbstracter;

public class InterfaceFieldAbstracter extends FieldAbstracter {
	public InterfaceFieldAbstracter(AbstractAbstracter abstracter, Field member, boolean impl) {
		super(abstracter, member, impl);
	}

	@Override
	protected boolean isConstant(Header header) {
		return Modifier.isFinal(header.access) && Modifier.isStatic(header.access);
	}
}
