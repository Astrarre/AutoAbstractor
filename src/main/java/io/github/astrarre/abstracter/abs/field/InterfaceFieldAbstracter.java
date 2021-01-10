package io.github.astrarre.abstracter.abs.field;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.abs.AbstractAbstracter;

public class InterfaceFieldAbstracter extends FieldAbstracter {
	public InterfaceFieldAbstracter(AbstracterConfig config, AbstractAbstracter abstracter, Field member, boolean impl) {
		super(config, abstracter, member, impl);
	}

	@Override
	protected boolean isConstant(Header header) {
		return Modifier.isFinal(header.access) && Modifier.isStatic(header.access);
	}
}
