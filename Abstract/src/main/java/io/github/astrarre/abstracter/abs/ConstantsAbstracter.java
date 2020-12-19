package io.github.astrarre.abstracter.abs;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import io.github.astrarre.abstracter.func.elements.ConstructorSupplier;
import io.github.astrarre.abstracter.func.elements.FieldSupplier;
import io.github.astrarre.abstracter.func.elements.MethodSupplier;
import io.github.astrarre.abstracter.func.inheritance.InterfaceFunction;
import io.github.astrarre.abstracter.util.asm.FieldUtil;
import org.objectweb.asm.tree.ClassNode;

public class ConstantsAbstracter extends InterfaceAbstracter {
	public ConstantsAbstracter(Class<?> cls,
			String name,
			FieldSupplier fieldSupplier) {
		super(cls, name, InterfaceFunction.EMPTY, ConstructorSupplier.EMPTY, fieldSupplier, MethodSupplier.EMPTY);
	}

	public ConstantsAbstracter(Class<?> cls) {
		this(cls, 0);
	}

	public ConstantsAbstracter(Class<?> cls, int version) {
		this(cls, getName(cls, "Minecraft", version));
	}

	public ConstantsAbstracter(Class<?> cls, String name) {
		this(cls, name, FieldSupplier.CONSTANTS);
	}

	@Override
	public void abstractField(ClassNode node, Field field, boolean impl) {
		int access = field.getModifiers();
		if(Modifier.isStatic(access)) {
			FieldUtil.createConstant(node, this.cls, field, impl);
		}
	}
}
