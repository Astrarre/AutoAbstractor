package io.github.astrarre.abstracter.abs;

import io.github.astrarre.abstracter.func.elements.ConstructorSupplier;
import io.github.astrarre.abstracter.func.elements.FieldSupplier;
import io.github.astrarre.abstracter.func.elements.MethodSupplier;
import io.github.astrarre.abstracter.func.inheritance.InterfaceFunction;

public class ConstantsAbstracter extends InterfaceAbstracter {
	public ConstantsAbstracter(Class<?> cls,
			String name,
			FieldSupplier fieldSupplier) {
		super(cls, name, InterfaceFunction.EMPTY, ConstructorSupplier.EMPTY, fieldSupplier, MethodSupplier.EMPTY);
	}

	public ConstantsAbstracter(Class<?> cls) {
		this(cls, getName(cls, "C", 0));
	}

	public ConstantsAbstracter(Class<?> cls, String name) {
		this(cls, name, FieldSupplier.CONSTANTS);
	}
}
