package io.github.f2bb.abstracter;

import java.util.function.IntUnaryOperator;

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
import io.github.f2bb.abstracter.func.serialization.SerializingFunction;
import io.github.f2bb.abstracter.func.string.ToStringFunction;
import org.objectweb.asm.tree.ClassNode;

public class AbstracterBuilder {
	private HeaderFunction<ClassNode> asm;
	private HeaderFunction<TypeSpec.Builder> java;
	private ConstructorSupplier supplier;
	private FieldSupplier fieldSupplier;
	private MethodSupplier methodSupplier;
	private InterfaceFunction interfaceFunction;
	private SuperFunction superFunction;
	private ToStringFunction<Class<?>> nameFunction;
	private IntUnaryOperator accessOperator;
	private FieldAbstracter<ClassNode> fieldAbstracterAsm;
	private MethodAbstracter<ClassNode> methodAbstracterAsm;
	private ConstructorAbstracter<ClassNode> constructorAbstracterAsm;
	private SerializingFunction<ClassNode> serializerAsm;
	private FieldAbstracter<TypeSpec.Builder> fieldAbstracterJava;
	private MethodAbstracter<TypeSpec.Builder> methodAbstracterJava;
	private ConstructorAbstracter<TypeSpec.Builder> constructorAbstracterJava;
	private SerializingFunction<TypeSpec.Builder> serializerJava;

	public AbstracterBuilder setAsm(HeaderFunction<ClassNode> asm) {
		this.asm = asm;
		return this;
	}

	public AbstracterBuilder setJava(HeaderFunction<TypeSpec.Builder> java) {
		this.java = java;
		return this;
	}

	public AbstracterBuilder setSupplier(ConstructorSupplier supplier) {
		this.supplier = supplier;
		return this;
	}

	public AbstracterBuilder setFieldSupplier(FieldSupplier fieldSupplier) {
		this.fieldSupplier = fieldSupplier;
		return this;
	}

	public AbstracterBuilder setMethodSupplier(MethodSupplier methodSupplier) {
		this.methodSupplier = methodSupplier;
		return this;
	}

	public AbstracterBuilder setInterfaceFunction(InterfaceFunction interfaceFunction) {
		this.interfaceFunction = interfaceFunction;
		return this;
	}

	public AbstracterBuilder setSuperFunction(SuperFunction superFunction) {
		this.superFunction = superFunction;
		return this;
	}

	public AbstracterBuilder setNameFunction(ToStringFunction<Class<?>> nameFunction) {
		this.nameFunction = nameFunction;
		return this;
	}

	public AbstracterBuilder setAccessOperator(IntUnaryOperator accessOperator) {
		this.accessOperator = accessOperator;
		return this;
	}

	public AbstracterBuilder setFieldAbstracterAsm(FieldAbstracter<ClassNode> fieldAbstracterAsm) {
		this.fieldAbstracterAsm = fieldAbstracterAsm;
		return this;
	}

	public AbstracterBuilder setMethodAbstracterAsm(MethodAbstracter<ClassNode> methodAbstracterAsm) {
		this.methodAbstracterAsm = methodAbstracterAsm;
		return this;
	}

	public AbstracterBuilder setConstructorAbstracterAsm(ConstructorAbstracter<ClassNode> constructorAbstracterAsm) {
		this.constructorAbstracterAsm = constructorAbstracterAsm;
		return this;
	}

	public AbstracterBuilder setSerializerAsm(SerializingFunction<ClassNode> serializerAsm) {
		this.serializerAsm = serializerAsm;
		return this;
	}

	public AbstracterBuilder setFieldAbstracterJava(FieldAbstracter<TypeSpec.Builder> fieldAbstracterJava) {
		this.fieldAbstracterJava = fieldAbstracterJava;
		return this;
	}

	public AbstracterBuilder setMethodAbstracterJava(MethodAbstracter<TypeSpec.Builder> methodAbstracterJava) {
		this.methodAbstracterJava = methodAbstracterJava;
		return this;
	}

	public AbstracterBuilder setConstructorAbstracterJava(ConstructorAbstracter<TypeSpec.Builder> constructorAbstracterJava) {
		this.constructorAbstracterJava = constructorAbstracterJava;
		return this;
	}

	public AbstracterBuilder setSerializerJava(SerializingFunction<TypeSpec.Builder> serializerJava) {
		this.serializerJava = serializerJava;
		return this;
	}

	public Abstracter build() {
		return new Abstracter(asm,
				java,
				supplier,
				fieldSupplier,
				methodSupplier,
				interfaceFunction,
				superFunction,
				nameFunction,
				accessOperator,
				fieldAbstracterAsm,
				methodAbstracterAsm,
				constructorAbstracterAsm,
				serializerAsm,
				fieldAbstracterJava,
				methodAbstracterJava,
				constructorAbstracterJava,
				serializerJava);
	}
}