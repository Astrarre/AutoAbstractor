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
import org.objectweb.asm.tree.AnnotationNode;
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
			FieldUtil.createConstant(node, this.cls, field, impl);
		} else {
			if (!Modifier.isFinal(access)) {
				MethodNode setter = FieldUtil.createSetter(this.cls, field, impl);
				if(!MethodUtil.conflicts(setter.name, setter.desc, node)) {
					this.addFieldRefAnnotation(setter, field);
					setter.access &= ~ACC_FINAL;
					node.methods.add(setter);
				}
			}

			MethodNode getter = FieldUtil.createGetter(this.cls, field, impl);
			if(!MethodUtil.conflicts(getter.name, getter.desc, node)) {
				this.addFieldRefAnnotation(getter, field);
				getter.access &= ~ACC_FINAL;
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
}
