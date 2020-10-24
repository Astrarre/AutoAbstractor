package io.github.f2bb.abstracter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.IntUnaryOperator;
import java.util.zip.ZipOutputStream;

import io.github.f2bb.abstracter.func.abstracting.constructor.ConstructorAbstracter;
import io.github.f2bb.abstracter.func.abstracting.field.FieldAbstracter;
import io.github.f2bb.abstracter.func.abstracting.method.MethodAbstracter;
import io.github.f2bb.abstracter.func.elements.ConstructorSupplier;
import io.github.f2bb.abstracter.func.elements.FieldSupplier;
import io.github.f2bb.abstracter.func.elements.MethodSupplier;
import io.github.f2bb.abstracter.func.header.HeaderFunction;
import io.github.f2bb.abstracter.func.inheritance.InterfaceFunction;
import io.github.f2bb.abstracter.func.inheritance.SuperFunction;
import io.github.f2bb.abstracter.func.serialization.SerializingFunction;
import io.github.f2bb.abstracter.func.string.ToStringFunction;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class Abstracter<T> implements Opcodes {
	private static final int INTERFACE_ADD = ACC_INTERFACE | ACC_ABSTRACT;
	private static final IntUnaryOperator INTERFACE_OPERATOR = i -> (i & (~(ACC_ENUM | ACC_FINAL))) | INTERFACE_ADD;
	public static final Abstracter<ClassNode> INTERFACE_IMPL_ASM = new Builder<ClassNode>()
			                                                               .headerFunction(HeaderFunction.ASM)
			                                                               .ctorSupplier(ConstructorSupplier.INTERFACE_DEFAULT)
			                                                               .fieldSupplier(FieldSupplier.INTERFACE_DEFAULT)
			                                                               .methodSupplier(MethodSupplier.INTERFACE_DEFAULT)
			                                                               .interfaceFunction(InterfaceFunction.INTERFACE_DEFAULT)
			                                                               .superFunction(SuperFunction.INTERFACE_DEFAULT)
			                                                               .nameFunction(ToStringFunction.INTERFACE_DEFAULT)
			                                                               .accessOperator(INTERFACE_OPERATOR)
			                                                               .fieldAbstracter(FieldAbstracter.INTERFACE_IMPL_ASM)
			                                                               .methodAbstracter(MethodAbstracter.INTERFACE_IMPL_ASM)
			                                                               .constructorAbstracter(ConstructorAbstracter.INTERFACE_ASM)
			                                                               .serializer(SerializingFunction.ASM).build();
	public static final Abstracter<ClassNode> INTERFACE_API_ASM = new Builder<ClassNode>()
			                                                              .headerFunction(HeaderFunction.ASM)
			                                                              .ctorSupplier(ConstructorSupplier.INTERFACE_DEFAULT)
			                                                              .fieldSupplier(FieldSupplier.INTERFACE_DEFAULT)
			                                                              .methodSupplier(MethodSupplier.INTERFACE_DEFAULT)
			                                                              .interfaceFunction(InterfaceFunction.INTERFACE_DEFAULT)
			                                                              .superFunction(SuperFunction.INTERFACE_DEFAULT)
			                                                              .nameFunction(ToStringFunction.INTERFACE_DEFAULT)
			                                                              .accessOperator(INTERFACE_OPERATOR)
			                                                              .fieldAbstracter(FieldAbstracter.INTERFACE_API_ASM)
			                                                              .methodAbstracter(MethodAbstracter.INTERFACE_API_ASM)
			                                                              .constructorAbstracter(ConstructorAbstracter.INTERFACE_API_ASM)
			                                                              .serializer(SerializingFunction.ASM).build();

	public static final Abstracter<ClassNode> BASE_IMPL_ASM = new Builder<ClassNode>()
			                                                          .headerFunction(HeaderFunction.ASM)
			                                                          .ctorSupplier(ConstructorSupplier.BASE_DEFAULT)
			                                                          .fieldSupplier(FieldSupplier.BASE_DEFAULT)
			                                                          .methodSupplier(MethodSupplier.BASE_DEFAULT)
			                                                          .interfaceFunction(InterfaceFunction.BASE_DEFAULT)
			                                                          .superFunction(SuperFunction.BASE_IMPL_DEFAULT)
			                                                          .nameFunction(ToStringFunction.BASE_DEFAULT)
			                                                          .accessOperator(IntUnaryOperator.identity())
			                                                          .fieldAbstracter(FieldAbstracter.BASE_IMPL_ASM)
			                                                          .methodAbstracter(MethodAbstracter.BASE_IMPL_ASM)
			                                                          .constructorAbstracter(ConstructorAbstracter.BASE_IMPL_ASM)
			                                                          .serializer(SerializingFunction.ASM).build();
	public static final Abstracter<ClassNode> BASE_API_ASM =
			new Builder<ClassNode>().headerFunction(HeaderFunction.ASM)
	                                                                                 .ctorSupplier(ConstructorSupplier.BASE_DEFAULT)
	                                                                                 .fieldSupplier(FieldSupplier.BASE_DEFAULT)
	                                                                                 .methodSupplier(MethodSupplier.BASE_DEFAULT)
	                                                                                 .interfaceFunction(
			                                                                                 InterfaceFunction.BASE_DEFAULT)
	                                                                                 .superFunction(SuperFunction.BASE_API_DEFAULT)
	                                                                                 .nameFunction(ToStringFunction.BASE_DEFAULT)
	                                                                                 .accessOperator(IntUnaryOperator
			                                                                                                 .identity())
	                                                                                 .fieldAbstracter(FieldAbstracter.BASE_API_ASM)
	                                                                                 .methodAbstracter(MethodAbstracter.BASE_API_ASM)
	                                                                                 .constructorAbstracter(
			                                                                                 ConstructorAbstracter.BASE_API_ASM)
	                                                                                 .serializer(SerializingFunction.ASM).build();

	protected final HeaderFunction<T> headerFunction;
	protected final ConstructorSupplier constructorSupplier;
	protected final FieldSupplier fieldSupplier;
	protected final MethodSupplier methodSupplier;
	protected final InterfaceFunction interfaceFunction;
	protected final SuperFunction superFunction;
	protected final ToStringFunction<Class<?>> nameFunction;
	protected final IntUnaryOperator accessOperator;
	protected final FieldAbstracter<T> fieldAbstracter;
	protected final MethodAbstracter<T> methodAbstracter;
	protected final ConstructorAbstracter<T> constructorAbstracter;
	protected final SerializingFunction<T> serializer;

	protected Abstracter(HeaderFunction<T> headerFunction,
			ConstructorSupplier supplier,
			FieldSupplier fieldSupplier,
			MethodSupplier methodSupplier,
			InterfaceFunction function,
			SuperFunction superFunction,
			ToStringFunction<Class<?>> nameFunction,
			IntUnaryOperator operator,
			FieldAbstracter<T> abstracter,
			MethodAbstracter<T> methodAbstracter,
			ConstructorAbstracter<T> constructorAbstracter,
			SerializingFunction<T> serializer) {
		this.headerFunction = headerFunction;
		this.constructorSupplier = supplier;
		this.fieldSupplier = fieldSupplier;
		this.methodSupplier = methodSupplier;
		this.interfaceFunction = function;
		this.superFunction = superFunction;
		this.nameFunction = nameFunction;
		this.accessOperator = operator;
		this.fieldAbstracter = abstracter;
		this.methodAbstracter = methodAbstracter;
		this.constructorAbstracter = constructorAbstracter;
		this.serializer = serializer;
	}

	public T apply(Class<?> cls, boolean impl) {
		T header = this.headerFunction.createHeader(this.accessOperator.applyAsInt(cls.getModifiers()),
				this.nameFunction.toString(cls),
				cls.getTypeParameters(),
				this.superFunction.findValidSuper(cls, impl),
				this.interfaceFunction.getInterfaces(cls));

		for (Field field : this.fieldSupplier.getFields(cls)) {
			this.fieldAbstracter.abstractField(header, cls, field, impl);
		}

		for (Constructor<?> constructor : this.constructorSupplier.getConstructors(cls)) {
			this.constructorAbstracter.abstractConstructor(header, cls, constructor, impl);
		}

		for (Method method : this.methodSupplier.getMethods(cls)) {
			this.methodAbstracter.abstractMethod(header, cls, method, impl);
		}

		return header;
	}

	public void serialize(ZipOutputStream out, Class<?> cls, T obj) {
		try {
			this.serializer.serialize(out, cls, obj);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Builder<T> asBuilder() {
		return new Builder<>(this.headerFunction,
				this.constructorSupplier,
				this.fieldSupplier,
				this.methodSupplier,
				this.interfaceFunction,
				this.superFunction,
				this.nameFunction,
				this.accessOperator,
				this.fieldAbstracter,
				this.methodAbstracter,
				this.constructorAbstracter,
				this.serializer);
	}

	public static class Builder<T> {
		private HeaderFunction<T> headerFunction;
		private ConstructorSupplier ctorSupplier;
		private FieldSupplier fieldSupplier;
		private MethodSupplier methodSupplier;
		private InterfaceFunction interfaceFunction;
		private SuperFunction superFunction;
		private ToStringFunction<Class<?>> nameFunction;
		private IntUnaryOperator accessOperator;
		private FieldAbstracter<T> fieldAbstracter;
		private MethodAbstracter<T> methodAbstracter;
		private ConstructorAbstracter<T> constructorAbstracter;
		private SerializingFunction<T> serializer;

		public Builder() {}
		private Builder(HeaderFunction<T> headerFunction,
				ConstructorSupplier ctorSupplier,
				FieldSupplier fieldSupplier,
				MethodSupplier methodSupplier,
				InterfaceFunction interfaceFunction,
				SuperFunction superFunction,
				ToStringFunction<Class<?>> nameFunction,
				IntUnaryOperator accessOperator,
				FieldAbstracter<T> fieldAbstracter,
				MethodAbstracter<T> methodAbstracter,
				ConstructorAbstracter<T> constructorAbstracter,
				SerializingFunction<T> serializer) {
			this.headerFunction = headerFunction;
			this.ctorSupplier = ctorSupplier;
			this.fieldSupplier = fieldSupplier;
			this.methodSupplier = methodSupplier;
			this.interfaceFunction = interfaceFunction;
			this.superFunction = superFunction;
			this.nameFunction = nameFunction;
			this.accessOperator = accessOperator;
			this.fieldAbstracter = fieldAbstracter;
			this.methodAbstracter = methodAbstracter;
			this.constructorAbstracter = constructorAbstracter;
			this.serializer = serializer;
		}

		public Builder<T> headerFunction(HeaderFunction<T> headerFunction) {
			this.headerFunction = headerFunction;
			return this;
		}

		public Builder<T> ctorSupplier(ConstructorSupplier ctorSupplier) {
			this.ctorSupplier = ctorSupplier;
			return this;
		}

		public Builder<T> fieldSupplier(FieldSupplier fieldSupplier) {
			this.fieldSupplier = fieldSupplier;
			return this;
		}

		public Builder<T> methodSupplier(MethodSupplier methodSupplier) {
			this.methodSupplier = methodSupplier;
			return this;
		}

		public Builder<T> interfaceFunction(InterfaceFunction interfaceFunction) {
			this.interfaceFunction = interfaceFunction;
			return this;
		}

		public Builder<T> superFunction(SuperFunction superFunction) {
			this.superFunction = superFunction;
			return this;
		}

		public Builder<T> nameFunction(ToStringFunction<Class<?>> nameFunction) {
			this.nameFunction = nameFunction;
			return this;
		}

		public Builder<T> accessOperator(IntUnaryOperator accessOperator) {
			this.accessOperator = accessOperator;
			return this;
		}

		public Builder<T> fieldAbstracter(FieldAbstracter<T> fieldAbstracter) {
			this.fieldAbstracter = fieldAbstracter;
			return this;
		}

		public Builder<T> methodAbstracter(MethodAbstracter<T> methodAbstracter) {
			this.methodAbstracter = methodAbstracter;
			return this;
		}

		public Builder<T> constructorAbstracter(ConstructorAbstracter<T> constructorAbstracter) {
			this.constructorAbstracter = constructorAbstracter;
			return this;
		}

		public Builder<T> serializer(SerializingFunction<T> serializer) {
			this.serializer = serializer;
			return this;
		}

		public Abstracter<T> build() {
			return new Abstracter<>(this.headerFunction,
					this.ctorSupplier,
					this.fieldSupplier,
					this.methodSupplier,
					this.interfaceFunction,
					this.superFunction,
					this.nameFunction,
					this.accessOperator,
					this.fieldAbstracter,
					this.methodAbstracter,
					this.constructorAbstracter,
					this.serializer);
		}
	}
}
