package io.github.f2bb.abstracter.abs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;

import io.github.f2bb.abstracter.func.elements.ConstructorSupplier;
import io.github.f2bb.abstracter.func.elements.FieldSupplier;
import io.github.f2bb.abstracter.func.elements.MethodSupplier;
import io.github.f2bb.abstracter.func.inheritance.InterfaceFunction;
import io.github.f2bb.abstracter.func.inheritance.SuperFunction;
import io.github.f2bb.abstracter.util.reflect.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

@SuppressWarnings ("UnstableApiUsage")
public abstract class AbstractAbstracter implements Opcodes {
	public final String name;
	protected final Class<?> cls;
	protected final InterfaceFunction interfaces;
	protected final SuperFunction superFunction;
	protected final ConstructorSupplier constructorSupplier;
	protected final FieldSupplier fieldSupplier;
	protected final MethodSupplier methodSupplier;

	protected AbstractAbstracter(Class<?> cls,
			String name,
			InterfaceFunction interfaces,
			SuperFunction function,
			ConstructorSupplier supplier,
			FieldSupplier fieldSupplier,
			MethodSupplier methodSupplier) {
		this.cls = cls;
		this.name = name;
		this.interfaces = interfaces;
		this.superFunction = function;
		this.constructorSupplier = supplier;
		this.fieldSupplier = fieldSupplier;
		this.methodSupplier = methodSupplier;
	}

	/**
	 * @return get a class's access flags
	 */
	public abstract int getAccess();
	public abstract void abstractField(ClassNode node, Field field, boolean impl);
	public abstract void abstractMethod(ClassNode node, Method method, boolean impl);
	public abstract void abstractConstructor(ClassNode node, Constructor<?> constructor, boolean impl);
	public abstract void postProcess(ClassNode node, boolean impl);


	public ClassNode apply(boolean impl) {
		ClassNode header = new ClassNode();
		header.version = V1_8;
		header.access = this.getAccess();
		header.name = this.name;
		Collection<Type> interfaces = this.interfaces.getInterfaces(this.cls);
		for (Type iface : interfaces) {
			header.interfaces.add(TypeUtil.getRawName(iface));
		}

		Type sup = this.superFunction.findValidSuper(this.cls, impl);
		header.superName = TypeUtil.getRawName(sup);
		header.signature = TypeUtil.classSignature(this.cls.getTypeParameters(), sup, interfaces);

		for (Field field : this.fieldSupplier.getFields(this.cls)) {
			this.abstractField(header, field, impl);
		}

		for (Constructor<?> constructor : this.constructorSupplier.getConstructors(this.cls)) {
			this.abstractConstructor(header, constructor, impl);
		}

		for (Method method : this.methodSupplier.getMethods(this.cls)) {
			this.abstractMethod(header, method, impl);
		}

		this.postProcess(header, impl);
		return header;
	}

	static String getName(Class<?> cls, String prefix, int version) {
		String str = org.objectweb.asm.Type.getInternalName(cls);
		str = str.replace("net/minecraft/", "v" + version + "/io/github/f2bb/");
		int last = str.lastIndexOf('/') + 1;
		return str.substring(0, last) + prefix + str.substring(last);
	}
}
