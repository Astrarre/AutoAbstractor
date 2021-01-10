package io.github.astrarre.abstracter.abs;

import static io.github.astrarre.abstracter.util.ArrayUtil.map;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;

import io.github.astrarre.abstracter.abs.field.FieldAbstracter;
import io.github.astrarre.abstracter.abs.field.InterfaceFieldAbstracter;
import io.github.astrarre.abstracter.abs.method.InterfaceConstructorAbstracter;
import io.github.astrarre.abstracter.abs.method.InterfaceMethodAbstracter;
import io.github.astrarre.abstracter.abs.method.MethodAbstracter;
import io.github.astrarre.abstracter.func.elements.ConstructorSupplier;
import io.github.astrarre.abstracter.func.elements.FieldSupplier;
import io.github.astrarre.abstracter.func.elements.MethodSupplier;
import io.github.astrarre.abstracter.func.inheritance.InterfaceFunction;
import io.github.astrarre.abstracter.func.inheritance.SuperFunction;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

@SuppressWarnings ("UnstableApiUsage")
public class InterfaceAbstracter extends AbstractAbstracter {
	private static final int REMOVE_FLAGS = ACC_ENUM | ACC_FINAL;
	private static final int ADD_FLAGS = ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT;

	public InterfaceAbstracter(Class<?> cls) {
		this(cls,
				getName(cls, "", 0),
				InterfaceFunction.INTERFACE_DEFAULT,
				ConstructorSupplier.INTERFACE_DEFAULT,
				FieldSupplier.INTERFACE_DEFAULT,
				MethodSupplier.INTERFACE_DEFAULT);
	}

	public InterfaceAbstracter(Class<?> cls,
			String name,
			InterfaceFunction interfaces,
			ConstructorSupplier supplier,
			FieldSupplier fieldSupplier,
			MethodSupplier methodSupplier) {
		super(cls, name, interfaces, SuperFunction.EMPTY, supplier, fieldSupplier, methodSupplier);
	}

	public InterfaceAbstracter(Class<?> cls, int version) {
		this(cls, getName(cls, "", version));
	}

	public InterfaceAbstracter(Class<?> cls, String name) {
		this(cls,
				name,
				InterfaceFunction.INTERFACE_DEFAULT,
				ConstructorSupplier.INTERFACE_DEFAULT,
				FieldSupplier.INTERFACE_DEFAULT,
				MethodSupplier.INTERFACE_DEFAULT);
	}

	@Override
	public void castToMinecraft(MethodVisitor visitor, Consumer<MethodVisitor> apply, Location parameter) {
		apply.accept(visitor);
		visitor.visitTypeInsn(CHECKCAST, this.name);
	}

	@Override
	public void castToCurrent(MethodVisitor visitor, Consumer<MethodVisitor> apply, Location parameter) {
		apply.accept(visitor);
	}

	@Override
	public int getAccess(int modifiers) {
		return (modifiers & ~REMOVE_FLAGS) | ADD_FLAGS;
	}

	@Override
	public MethodAbstracter<Constructor<?>> abstractConstructor(Constructor<?> constructor, boolean impl) {
		return new InterfaceConstructorAbstracter(this, constructor, impl);
	}

	@Override
	public MethodAbstracter<Method> abstractMethod(Method method, boolean impl) {
		return new InterfaceMethodAbstracter(this, method, impl);
	}

	@Override
	public FieldAbstracter abstractField(Field field, boolean impl) {
		return new InterfaceFieldAbstracter(this, field, impl);
	}


}
