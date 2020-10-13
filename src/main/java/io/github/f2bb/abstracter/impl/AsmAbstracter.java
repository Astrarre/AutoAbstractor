package io.github.f2bb.abstracter.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.function.IntUnaryOperator;

import io.github.f2bb.abstracter.AbstractAbstracter;
import io.github.f2bb.abstracter.func.abstracting.FieldAbstracter;
import io.github.f2bb.abstracter.func.elements.ConstructorSupplier;
import io.github.f2bb.abstracter.func.elements.FieldSupplier;
import io.github.f2bb.abstracter.func.elements.MethodSupplier;
import io.github.f2bb.abstracter.func.inheritance.InterfaceFunction;
import io.github.f2bb.abstracter.func.inheritance.SuperFunction;
import io.github.f2bb.abstracter.func.string.ToStringFunction;
import io.github.f2bb.abstracter.util.AbstracterUtil;
import io.github.f2bb.abstracter.util.RawClassType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.tree.ClassNode;

public abstract class AsmAbstracter extends AbstractAbstracter<ClassNode> implements Opcodes {
	protected AsmAbstracter(ConstructorSupplier supplier,
			FieldSupplier fieldSupplier,
			MethodSupplier methodSupplier,
			InterfaceFunction function,
			SuperFunction superFunction,
			ToStringFunction<Class<?>> nameFunction,
			IntUnaryOperator operator,
			FieldAbstracter<ClassNode> abstracter) {
		super(supplier, fieldSupplier, methodSupplier, function, superFunction, nameFunction, operator, abstracter);
	}

	@Override
	protected ClassNode createHeader(int access,
			String name,
			TypeVariable<?>[] variables,
			Type sup,
			Collection<Type> interfaces) {
		ClassNode node = new ClassNode();
		node.visit(ASM9,
				access,
				name,
				classSignature(variables, sup, interfaces),
				AbstracterUtil.getRawName(sup),
				interfaces.stream().map(AbstracterUtil::getRawName).toArray(String[]::new));
		return node;
	}

	public static String classSignature(TypeVariable<?>[] variables, Type superClass, Collection<Type> interfaces) {
		SignatureWriter writer = new SignatureWriter();
		visit(writer, variables);
		visit(writer.visitSuperclass(), superClass);
		for (Type iface : interfaces) {
			visit(writer.visitInterface(), iface);
		}
		return writer.toString();
	}

	@Override
	protected void abstractMethod(ClassNode header, Class<?> cls, Method field) {

	}

	@Override
	protected void abstractConstructor(ClassNode header, Class<?> cls, Constructor<?> field) {

	}

	public static void visit(SignatureVisitor visitor, TypeVariable<?>[] variables) {
		for (TypeVariable<?> variable : variables) {
			visit(visitor, variable);
		}
	}

	public static void visit(SignatureVisitor visitor, TypeVariable<?> variable) {
		visitor.visitFormalTypeParameter(variable.getName());
		boolean first = true;
		for (Type bound : variable.getBounds()) {
			if (first) {
				visitor.visitClassBound();
				first = false;
			} else {
				visitor.visitInterfaceBound();
			}
			visit(visitor, bound);
		}
	}

	public static void visit(SignatureVisitor visitor, Type type) {
		if (type instanceof Class<?>) {
			Class<?> c = (Class<?>) type;
			if (c.isPrimitive()) {
				visitor.visitBaseType(org.objectweb.asm.Type.getDescriptor(c).charAt(0));
			} else {
				// todo remap? no cus one method relies on the visitor crashing only if the class is right
				visitor.visitClassType(org.objectweb.asm.Type.getInternalName(c));
				visitor.visitEnd();
			}
			return;
		} else if (type instanceof GenericArrayType) {
			visit(visitor.visitArrayType(), ((GenericArrayType) type).getGenericComponentType());
			return;
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			// visit the type
			Class<?> raw = (Class<?>) pt.getRawType();
			if (raw.isPrimitive()) {
				visitor.visitBaseType(org.objectweb.asm.Type.getDescriptor(raw).charAt(0));
			} else {
				visitor.visitClassType(org.objectweb.asm.Type.getInternalName(raw));
			}

			Type[] args = pt.getActualTypeArguments();
			for (Type arg : args) {
				if (arg instanceof Class<?>) {
					visitor.visitTypeArgument('=');
				}
				visit(visitor, arg);
			}
			visitor.visitEnd();
			return;
		} else if (type instanceof TypeVariable<?>) {
			visitor.visitTypeVariable(((TypeVariable<?>) type).getName());
			return;
		} else if (type instanceof WildcardType) {
			WildcardType wt = (WildcardType) type;
			Type[] array = wt.getLowerBounds();
			if (array.length > 0) {
				visitor.visitTypeArgument('-');
			} else {
				array = wt.getUpperBounds();
				if (array.length == 1 && array[0] == Object.class) {
					visitor.visitTypeArgument('*');
				} else {
					visitor.visitTypeArgument('+');
				}
			}

			for (Type l : array) {
				visit(visitor, l);
			}
			return;
		} else if (type instanceof RawClassType) {
			SignatureReader reader = new SignatureReader(type.getTypeName());
			reader.accept(visitor);
			return;
		}
		throw new IllegalArgumentException("Unrecognized type " + type + " " + type.getClass());
	}
}
