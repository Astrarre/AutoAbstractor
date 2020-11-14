package io.github.f2bb.abstracter.abs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;

import io.github.f2bb.abstracter.AbstracterConfig;
import io.github.f2bb.abstracter.Cls;
import io.github.f2bb.abstracter.func.elements.ConstructorSupplier;
import io.github.f2bb.abstracter.func.elements.FieldSupplier;
import io.github.f2bb.abstracter.func.elements.MethodSupplier;
import io.github.f2bb.abstracter.func.inheritance.InterfaceFunction;
import io.github.f2bb.abstracter.func.inheritance.SuperFunction;
import io.github.f2bb.abstracter.func.map.TypeMappingFunction;
import io.github.f2bb.abstracter.util.AbstracterLoader;
import io.github.f2bb.abstracter.util.asm.FieldUtil;
import io.github.f2bb.abstracter.util.asm.InvokeUtil;
import io.github.f2bb.abstracter.util.asm.MethodUtil;
import io.github.f2bb.abstracter.util.reflect.ReflectUtil;
import io.github.f2bb.abstracter.util.reflect.TypeUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class BaseAbstracter extends AbstractAbstracter {
	public BaseAbstracter(Class<?> cls) {
		this(cls,
				getName(cls, "Base", 0),
				InterfaceFunction.BASE_DEFAULT,
				SuperFunction.BASE_DEFAULT,
				ConstructorSupplier.BASE_DEFAULT,
				FieldSupplier.BASE_DEFAULT,
				MethodSupplier.BASE_DEFAULT);
	}

	public BaseAbstracter(Class<?> cls,
			String name,
			InterfaceFunction interfaces,
			SuperFunction function,
			ConstructorSupplier supplier,
			FieldSupplier fieldSupplier,
			MethodSupplier methodSupplier) {
		super(cls, name, interfaces, function, supplier, fieldSupplier, methodSupplier);
	}

	public BaseAbstracter(Class<?> c, String s) {
		this(c,
				s,
				InterfaceFunction.BASE_DEFAULT,
				SuperFunction.BASE_DEFAULT,
				ConstructorSupplier.BASE_DEFAULT,
				FieldSupplier.BASE_DEFAULT,
				MethodSupplier.BASE_DEFAULT);
	}

	@Override
	public int getAccess() {
		return this.cls.getModifiers();
	}

	@Override
	public void abstractField(ClassNode node, Field field, boolean impl) {
		if (AbstracterLoader.isMinecraft(TypeMappingFunction.raw(this.cls, field.getGenericType()))) {
			if (!Modifier.isFinal(field.getModifiers())) {
				MethodNode setter = FieldUtil.createSetter(this.cls, field, impl);
				if (!MethodUtil.conflicts(setter.name, setter.desc, node)) {
					node.methods.add(setter);
				}
			}
			MethodNode getter = FieldUtil.createGetter(this.cls, field, impl);
			if (!MethodUtil.conflicts(getter.name, getter.desc, node)) {
				node.methods.add(getter);
			}
		} else {
			if (!impl) {
				java.lang.reflect.Type reified = TypeMappingFunction.reify(this.cls, field.getGenericType());
				FieldNode fieldNode = new FieldNode(field.getModifiers(),
						field.getName(),
						Type.getDescriptor(field.getType()),
						TypeUtil.toSignature(reified),
						null);
				node.fields.add(fieldNode);
			}
		}
	}

	@Override
	public void abstractMethod(ClassNode node, Method method, boolean impl) {
		MethodUtil.abstractMethod(node, this.cls, method, impl, false);
	}

	@Override
	public void abstractConstructor(ClassNode node, Constructor<?> constructor, boolean impl) {
		String desc = Type.getConstructorDescriptor(constructor);
		MethodNode method = new MethodNode(constructor.getModifiers(),
				"<init>",
				TypeUtil.REMAPPER.mapSignature(desc, false),
				impl ? null : TypeUtil.REMAPPER.mapSignature(ReflectUtil.getSignature(constructor), false),
				null);
		if (impl) {
			InvokeUtil.invokeConstructor(method, constructor, false);
		} else {
			InvokeUtil.visitStub(method);
		}
		node.methods.add(method);
	}

	@Override
	public void postProcess(ClassNode node, boolean impl) {
		node.interfaces.add(AbstracterConfig.getInterfaceName(this.cls));
		if (node.signature != null) {
			// todo inner instance classes, god damnit
			String name = AbstracterConfig.getInterfaceName(this.cls);
			TypeVariable<?>[] variable = this.cls.getTypeParameters();
			StringBuilder signature = new StringBuilder(name.length() + 2 + variable.length * 3);
			signature.append('L').append(name);
			if(variable.length != 0) {
				signature.append('<');
				for (TypeVariable<?> var : variable) {
					signature.append('T').append(var.getName()).append(';');
				}
				signature.append('>');
			}
			signature.append(';');
			node.signature += signature;
		}
	}

	public Builder toBuild() {
		return new Builder(this.cls, this.name).interfaces(this.interfaces).superClass(this.superFunction)
		                                       .constructors(this.constructorSupplier).fields(this.fieldSupplier)
		                                       .methods(this.methodSupplier);
	}

	public static class Builder {
		private Class<?> cls;
		private String name;
		private InterfaceFunction interfaces = InterfaceFunction.BASE_DEFAULT;
		private SuperFunction function = SuperFunction.BASE_DEFAULT;
		private ConstructorSupplier supplier = ConstructorSupplier.BASE_DEFAULT;
		private FieldSupplier fieldSupplier = FieldSupplier.BASE_DEFAULT;
		private MethodSupplier methodSupplier = MethodSupplier.BASE_DEFAULT;

		public Builder(Class<?> cls) {
			this(cls, getName(cls, "Base", 0));
		}

		public Builder(Class<?> cls, String name) {
			this.cls = cls;
			this.name = name;
		}

		public Builder interfaces(InterfaceFunction interfaces) {
			this.interfaces = interfaces;
			return this;
		}

		public Builder superClass(SuperFunction function) {
			this.function = function;
			return this;
		}

		public Builder constructors(ConstructorSupplier supplier) {
			this.supplier = supplier;
			return this;
		}

		public Builder fields(FieldSupplier supplier) {
			this.fieldSupplier = supplier;
			return this;
		}

		public Builder methods(MethodSupplier supplier) {
			this.methodSupplier = supplier;
			return this;
		}

		public BaseAbstracter build() {
			return new BaseAbstracter(this.cls,
					this.name,
					this.interfaces,
					this.function,
					this.supplier,
					this.fieldSupplier,
					this.methodSupplier);
		}
	}
}
