package io.github.f2bb.abstracter.abs;

import io.github.f2bb.abstracter.func.elements.ConstructorSupplier;
import io.github.f2bb.abstracter.func.elements.FieldSupplier;
import io.github.f2bb.abstracter.func.elements.MethodSupplier;
import io.github.f2bb.abstracter.func.inheritance.InterfaceFunction;

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
