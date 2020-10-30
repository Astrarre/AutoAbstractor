package io.github.f2bb.abstracter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.IntUnaryOperator;
import java.util.zip.ZipOutputStream;

import com.squareup.javapoet.TypeSpec;
import io.github.f2bb.abstracter.func.abstracting.constructor.ConstructorAbstracter;
import io.github.f2bb.abstracter.func.abstracting.field.FieldAbstracter;
import io.github.f2bb.abstracter.func.abstracting.method.MethodAbstracter;
import io.github.f2bb.abstracter.func.elements.ConstructorSupplier;
import io.github.f2bb.abstracter.func.elements.FieldSupplier;
import io.github.f2bb.abstracter.func.elements.MethodSupplier;
import io.github.f2bb.abstracter.func.header.HeaderFunction;
import io.github.f2bb.abstracter.func.inheritance.InterfaceFunction;
import io.github.f2bb.abstracter.func.inheritance.SuperFunction;
import io.github.f2bb.abstracter.func.postprocess.PostProcessor;
import io.github.f2bb.abstracter.func.serialization.SerializingFunction;
import io.github.f2bb.abstracter.func.string.ToStringFunction;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class Abstracter implements Opcodes {
	public static final Abstracter BASE = new AbstracterBuilder().setAsm(HeaderFunction.getAsm(true))
	                                                             .setJava(HeaderFunction.getJava(true))
	                                                             .setSupplier(ConstructorSupplier.BASE_DEFAULT)
	                                                             .setFieldSupplier(FieldSupplier.BASE_DEFAULT)
	                                                             .setMethodSupplier(MethodSupplier.BASE_DEFAULT)
	                                                             .setInterfaceFunction(InterfaceFunction.BASE_DEFAULT)
	                                                             .setSuperFunction(SuperFunction.EMPTY)
	                                                             .setNameFunction(ToStringFunction.BASE_DEFAULT)
	                                                             .setAccessOperator(IntUnaryOperator.identity())
	                                                             .setFieldAbstracterAsm(FieldAbstracter.BASE_ASM)
	                                                             .setMethodAbstracterAsm(MethodAbstracter.BASE_ASM)
	                                                             .setConstructorAbstracterAsm(ConstructorAbstracter.BASE_ASM)
	                                                             .setSerializerAsm(SerializingFunction.ASM)
	                                                             .setFieldAbstracterJava(FieldAbstracter.BASE_JAVA)
	                                                             .setMethodAbstracterJava(MethodAbstracter.JAVA_BASE)
	                                                             .setConstructorAbstracterJava(ConstructorAbstracter.BASE_JAVA)
	                                                             .setSerializerJava(SerializingFunction.getJava(false))
	                                                             .build();
	private static final IntUnaryOperator INTERFACE_OPERATOR =
			i -> i & ~(ACC_ENUM | ACC_FINAL) | ACC_INTERFACE | ACC_ABSTRACT;
	public static final Abstracter INTERFACE = new AbstracterBuilder().setAsm(HeaderFunction.getAsm(false))
	                                                                  .setJava(HeaderFunction.getJava(false))
	                                                                  .setSupplier(ConstructorSupplier.INTERFACE_DEFAULT)
	                                                                  .setFieldSupplier(FieldSupplier.INTERFACE_DEFAULT)
	                                                                  .setMethodSupplier(MethodSupplier.INTERFACE_DEFAULT)
	                                                                  .setInterfaceFunction(InterfaceFunction.INTERFACE_DEFAULT)
	                                                                  .setSuperFunction(SuperFunction.EMPTY)
	                                                                  .setNameFunction(ToStringFunction.INTERFACE_DEFAULT)
	                                                                  .setAccessOperator(INTERFACE_OPERATOR)
	                                                                  .setFieldAbstracterAsm(FieldAbstracter.INTERFACE_ASM)
	                                                                  .setMethodAbstracterAsm(MethodAbstracter.INTERFACE_ASM)
	                                                                  .setConstructorAbstracterAsm(ConstructorAbstracter.INTERFACE_ASM)
	                                                                  .setSerializerAsm(SerializingFunction.ASM)
	                                                                  .setFieldAbstracterJava(FieldAbstracter.INTERFACE_JAVA)
	                                                                  .setMethodAbstracterJava(MethodAbstracter.JAVA_INTERFACE)
	                                                                  .setConstructorAbstracterJava(
			                                                                  ConstructorAbstracter.INTERFACE_JAVA)
	                                                                  .setSerializerJava(SerializingFunction
			                                                                                     .getJava(true))
	                                                                  .build();
	protected final HeaderFunction<ClassNode> headerFunctionAsm;
	protected final HeaderFunction<TypeSpec.Builder> headerFunctionJava;
	protected final ConstructorSupplier constructorSupplier;
	protected final FieldSupplier fieldSupplier;
	protected final MethodSupplier methodSupplier;
	protected final InterfaceFunction interfaceFunction;
	protected final SuperFunction superFunction;
	protected final ToStringFunction<Class<?>> nameFunction;
	protected final IntUnaryOperator accessOperator;
	protected final FieldAbstracter<ClassNode> fieldAbstracterAsm;
	protected final MethodAbstracter<ClassNode> methodAbstracterAsm;
	protected final ConstructorAbstracter<ClassNode> constructorAbstracterAsm;
	protected final SerializingFunction<ClassNode> serializerAsm;
	protected final FieldAbstracter<TypeSpec.Builder> fieldAbstracterJava;
	protected final MethodAbstracter<TypeSpec.Builder> methodAbstracterJava;
	protected final ConstructorAbstracter<TypeSpec.Builder> constructorAbstracterJava;
	protected final SerializingFunction<TypeSpec.Builder> serializerJava;
	protected final PostProcessor processor;

	public Abstracter(HeaderFunction<ClassNode> asm,
			HeaderFunction<TypeSpec.Builder> java,
			ConstructorSupplier supplier,
			FieldSupplier fieldSupplier,
			MethodSupplier methodSupplier,
			InterfaceFunction function,
			SuperFunction superFunction,
			ToStringFunction<Class<?>> nameFunction,
			IntUnaryOperator operator,
			FieldAbstracter<ClassNode> abstracterAsm,
			MethodAbstracter<ClassNode> methodAbstracterAsm,
			ConstructorAbstracter<ClassNode> constructorAbstracterAsm,
			SerializingFunction<ClassNode> serializerAsm,
			FieldAbstracter<TypeSpec.Builder> abstracterJava,
			MethodAbstracter<TypeSpec.Builder> methodAbstracterJava,
			ConstructorAbstracter<TypeSpec.Builder> constructorAbstracterJava,
			SerializingFunction<TypeSpec.Builder> serializerJava,
			PostProcessor processor) {
		this.headerFunctionAsm = asm;
		this.headerFunctionJava = java;
		this.constructorSupplier = supplier;
		this.fieldSupplier = fieldSupplier;
		this.methodSupplier = methodSupplier;
		this.interfaceFunction = function;
		this.superFunction = superFunction;
		this.nameFunction = nameFunction;
		this.accessOperator = operator;
		this.fieldAbstracterAsm = abstracterAsm;
		this.methodAbstracterAsm = methodAbstracterAsm;
		this.constructorAbstracterAsm = constructorAbstracterAsm;
		this.serializerAsm = serializerAsm;
		this.fieldAbstracterJava = abstracterJava;
		this.methodAbstracterJava = methodAbstracterJava;
		this.constructorAbstracterJava = constructorAbstracterJava;
		this.serializerJava = serializerJava;
		this.processor = processor;
	}

	public ClassNode applyAsm(Class<?> cls, boolean impl) {
		ClassNode header = this.headerFunctionAsm.createHeader(cls,
				this.accessOperator.applyAsInt(cls.getModifiers()),
				this.nameFunction.toString(cls),
				cls.getTypeParameters(),
				this.superFunction.findValidSuper(cls, impl),
				this.interfaceFunction.getInterfaces(cls));

		for (Field field : this.fieldSupplier.getFields(cls)) {
			this.fieldAbstracterAsm.abstractField(header, cls, field, impl);
		}

		for (Constructor<?> constructor : this.constructorSupplier.getConstructors(cls)) {
			this.constructorAbstracterAsm.abstractConstructor(header, cls, constructor, impl);
		}

		for (Method method : this.methodSupplier.getMethods(cls)) {
			this.methodAbstracterAsm.abstractMethod(header, cls, method, impl);
		}

		this.processor.processAsm(header, cls, impl);
		return header;
	}

	public TypeSpec.Builder applyJava(Class<?> cls, boolean impl) {
		TypeSpec.Builder header = this.headerFunctionJava.createHeader(cls,
				this.accessOperator.applyAsInt(cls.getModifiers()),
				this.nameFunction.toString(cls),
				cls.getTypeParameters(),
				this.superFunction.findValidSuper(cls, impl),
				this.interfaceFunction.getInterfaces(cls));

		for (Field field : this.fieldSupplier.getFields(cls)) {
			this.fieldAbstracterJava.abstractField(header, cls, field, impl);
		}

		for (Constructor<?> constructor : this.constructorSupplier.getConstructors(cls)) {
			this.constructorAbstracterJava.abstractConstructor(header, cls, constructor, impl);
		}

		for (Method method : this.methodSupplier.getMethods(cls)) {
			this.methodAbstracterJava.abstractMethod(header, cls, method, impl);
		}

		this.processor.processJava(header, cls, impl);
		return header;
	}

	public void serialize(ZipOutputStream out, Class<?> cls, ClassNode obj) {
		try {
			this.serializerAsm.serialize(out, cls, obj);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void serialize(ZipOutputStream out, Class<?> cls, TypeSpec.Builder builder) {
		try {
			this.serializerJava.serialize(out, cls, builder);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public AbstracterBuilder asBuilder() {
		return new AbstracterBuilder().setAsm(this.headerFunctionAsm)
		                              .setJava(this.headerFunctionJava)
		                              .setSupplier(this.constructorSupplier)
		                              .setFieldSupplier(this.fieldSupplier)
		                              .setMethodSupplier(this.methodSupplier)
		                              .setInterfaceFunction(this.interfaceFunction)
		                              .setSuperFunction(this.superFunction)
		                              .setNameFunction(this.nameFunction)
		                              .setAccessOperator(this.accessOperator)
		                              .setFieldAbstracterAsm(this.fieldAbstracterAsm)
		                              .setMethodAbstracterAsm(this.methodAbstracterAsm)
		                              .setConstructorAbstracterAsm(this.constructorAbstracterAsm)
		                              .setSerializerAsm(this.serializerAsm)
		                              .setFieldAbstracterJava(this.fieldAbstracterJava)
		                              .setMethodAbstracterJava(this.methodAbstracterJava)
		                              .setConstructorAbstracterJava(this.constructorAbstracterJava)
		                              .setSerializerJava(this.serializerJava)
		                              .setPostProcessor(PostProcessor.NOTHING);
	}
}
