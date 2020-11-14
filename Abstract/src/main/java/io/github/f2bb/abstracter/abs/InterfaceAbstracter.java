package io.github.f2bb.abstracter.abs;

import static io.github.f2bb.abstracter.util.ArrayUtil.map;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.function.Function;

import com.google.common.reflect.TypeToken;
import io.github.f2bb.abstracter.func.elements.ConstructorSupplier;
import io.github.f2bb.abstracter.func.elements.FieldSupplier;
import io.github.f2bb.abstracter.func.elements.MethodSupplier;
import io.github.f2bb.abstracter.func.inheritance.InterfaceFunction;
import io.github.f2bb.abstracter.func.inheritance.SuperFunction;
import io.github.f2bb.abstracter.func.map.TypeMappingFunction;
import io.github.f2bb.abstracter.util.asm.FieldUtil;
import io.github.f2bb.abstracter.util.asm.InvokeUtil;
import io.github.f2bb.abstracter.util.asm.MethodUtil;
import io.github.f2bb.abstracter.util.reflect.TypeUtil;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

@SuppressWarnings ("UnstableApiUsage")
public class InterfaceAbstracter extends AbstractAbstracter {
	public InterfaceAbstracter(Class<?> cls,
			String name,
			InterfaceFunction interfaces,
			SuperFunction function,
			ConstructorSupplier supplier,
			FieldSupplier fieldSupplier,
			MethodSupplier methodSupplier) {
		super(cls, name, interfaces, function, supplier, fieldSupplier, methodSupplier);
	}

	public InterfaceAbstracter(Class<?> cls) {
		this(cls,
				getName(cls, "I", 0),
				InterfaceFunction.INTERFACE_DEFAULT,
				SuperFunction.EMPTY,
				ConstructorSupplier.INTERFACE_DEFAULT,
				FieldSupplier.INTERFACE_DEFAULT,
				MethodSupplier.INTERFACE_DEFAULT);
	}

	public InterfaceAbstracter(Class<?> cls, String name) {
		this(cls,
				name,
				InterfaceFunction.INTERFACE_DEFAULT,
				SuperFunction.EMPTY,
				ConstructorSupplier.INTERFACE_DEFAULT,
				FieldSupplier.INTERFACE_DEFAULT,
				MethodSupplier.INTERFACE_DEFAULT);
	}

	@Override
	public int getAccess() {
		return ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT;
	}

	@Override
	public void abstractField(ClassNode node, Field field, boolean impl) {
		int access = field.getModifiers();
		if (Modifier.isStatic(access) && Modifier.isFinal(access)) {
			node.fields.add(FieldUtil.createConstant(node, this.cls, field, impl));
		} else {
			if (!Modifier.isFinal(access)) {
				MethodNode setter = FieldUtil.createSetter(this.cls, field, impl);
				if(!MethodUtil.conflicts(setter.name, setter.desc, node)) {
					node.methods.add(setter);
				}
			}

			MethodNode getter = FieldUtil.createGetter(this.cls, field, impl);
			if(!MethodUtil.conflicts(getter.name, getter.desc, node)) {
				node.methods.add(getter);
			}
		}
	}

	@Override
	public void abstractMethod(ClassNode node, Method method, boolean impl) {
		MethodUtil.abstractMethod(node, this.cls, method, impl, true);
	}

	@Override
	public void abstractConstructor(ClassNode node, Constructor<?> constructor, boolean impl) {
		Function<Type, TypeToken<?>> resolve = TypeMappingFunction.resolve(this.cls);
		TypeToken<?>[] params = map(constructor.getGenericParameterTypes(), resolve::apply, TypeToken[]::new);
		TypeToken<?> returnType = resolve.apply(this.cls);
		MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC,
				"newInstance",
				TypeUtil.methodDescriptor(params, returnType),
				TypeUtil.methodSignature(constructor.getTypeParameters(), params, returnType),
				null);
		if (impl) {
			InvokeUtil.invokeConstructor(method, constructor, true);
		} else {
			InvokeUtil.visitStub(method);
		}
		node.methods.add(method);
	}

	@Override
	public void postProcess(ClassNode node, boolean impl) {}

	public static class Builder {
		private Class<?> cls;
		private String name;
		private InterfaceFunction interfaces = InterfaceFunction.INTERFACE_DEFAULT;
		private SuperFunction function = SuperFunction.EMPTY;
		private ConstructorSupplier supplier = ConstructorSupplier.INTERFACE_DEFAULT;
		private FieldSupplier fieldSupplier = FieldSupplier.INTERFACE_DEFAULT;
		private MethodSupplier methodSupplier = MethodSupplier.INTERFACE_DEFAULT;

		public Builder(Class<?> cls, String name) {
			this.cls = cls;
			this.name = name;
		}

		public Builder(Class<?> cls) {
			this(cls, getName(cls, "I", 0));
		}

		public InterfaceAbstracter.Builder setInterfaces(InterfaceFunction interfaces) {
			this.interfaces = interfaces;
			return this;
		}

		public InterfaceAbstracter.Builder setFunction(SuperFunction function) {
			this.function = function;
			return this;
		}

		public InterfaceAbstracter.Builder setSupplier(ConstructorSupplier supplier) {
			this.supplier = supplier;
			return this;
		}

		public InterfaceAbstracter.Builder setFieldSupplier(FieldSupplier supplier) {
			this.fieldSupplier = supplier;
			return this;
		}

		public InterfaceAbstracter.Builder setMethodSupplier(MethodSupplier supplier) {
			this.methodSupplier = supplier;
			return this;
		}

		public InterfaceAbstracter build() {
			return new InterfaceAbstracter(this.cls,
					this.name, this.interfaces, this.function, this.supplier, this.fieldSupplier, this.methodSupplier);
		}
	}
}
