package io.github.f2bb.abstracter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import java.util.zip.ZipOutputStream;

import com.squareup.javapoet.TypeSpec;
import io.github.f2bb.abstracter.ex.InvalidClassException;
import io.github.f2bb.abstracter.func.abstracting.ConstructorAbstracter;
import io.github.f2bb.abstracter.func.abstracting.FieldAbstracter;
import io.github.f2bb.abstracter.func.abstracting.MethodAbstracter;
import io.github.f2bb.abstracter.func.elements.ConstructorSupplier;
import io.github.f2bb.abstracter.func.elements.FieldSupplier;
import io.github.f2bb.abstracter.func.elements.MethodSupplier;
import io.github.f2bb.abstracter.func.header.HeaderFunction;
import io.github.f2bb.abstracter.func.inheritance.InnerClassVisit;
import io.github.f2bb.abstracter.func.inheritance.InterfaceFunction;
import io.github.f2bb.abstracter.func.inheritance.SuperFunction;
import io.github.f2bb.abstracter.func.serialization.SerializingFunction;
import io.github.f2bb.abstracter.func.string.ToStringFunction;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SignatureRemapper;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.ClassNode;

public class Abstracter<T> implements Opcodes {
	public static final SignatureVisitor EMPTY_VISITOR = new SignatureVisitor(ASM9) {};
	private static final ClsLdr INSTANCE = new ClsLdr();
	public static final Remapper REMAPPER = new Remapper() {
		@Override
		public String map(String internalName) {
			Class<?> cls = Abstracter.getClass(Type.getObjectType(internalName).getClassName());
			return Abstracter.getInterfaceName(cls);
		}
	};

	public static <A> A[] add(A[] as, A a) {
		A[] copy = Arrays.copyOf(as, as.length + 1);
		copy[as.length] = a;
		return copy;
	}

	public static <A, B> B[] map(A[] arr, Function<A, B> func, IntFunction<B[]> array) {
		B[] bs = array.apply(arr.length);
		for (int i = 0; i < arr.length; i++) {
			bs[i] = func.apply(arr[i]);
		}
		return bs;
	}

	public static <A, B> List<B> map(A[] arr, Function<A, B> func) {
		ArrayList<B> array = new ArrayList<>(arr.length);
		for (A a : arr) {
			array.add(func.apply(a));
		}
		return array;
	}

	private static final class ClsLdr extends URLClassLoader {
		public ClsLdr() {
			super(new URL[] {});
		}

		@Override
		public void addURL(URL url) {
			super.addURL(url);
		}
	}

	public static Class<?> getClass(String reflectionName) {
		try {
			return INSTANCE.loadClass(reflectionName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getInterfaceName(Class<?> cls) {
		return getName(cls, "I", Type.getInternalName(cls));
	}

	public static String getBaseName(Class<?> cls) {
		return getName(cls, "Base", Type.getInternalName(cls));
	}

	public static String getInterfaceDesc(Class<?> cls) {
		return getName(cls, "I", Type.getDescriptor(cls));
	}

	public static boolean isMinecraft(Class<?> cls) {
		return cls.getClassLoader() == INSTANCE;
	}

	public static boolean isBaseAbstracted(Class<?> cls) {
		return isMinecraft(cls) && cls.getSimpleName().contains("Block"); // todo
	}

	public static boolean isAbstracted(Class<?> cls) {
		return isMinecraft(cls) && isAbstractedInternal(cls);
	}

	/**
	 * @return true if the class is a minecraft class, but isn't supposed to be abstracted
	 */
	public static boolean isUnabstractedClass(Class<?> cls) {
		return isMinecraft(cls) && !isAbstractedInternal(cls);
	}

	public static boolean isValid(String signature) {
		SignatureRemapper remapper = new SignatureRemapper(EMPTY_VISITOR, REMAPPER);
		SignatureReader reader = new SignatureReader(signature);
		try {
			reader.accept(remapper);
			return true;
		} catch (InvalidClassException e) {
			return false;
		}
	}

	private static String getName(Class<?> cls, String prefix, String str) {
		if (isAbstracted(cls)) {
			int last = str.lastIndexOf('/') + 1;
			return str.substring(0, last) + prefix + str.substring(last);
		} else if (isMinecraft(cls)) {
			throw new InvalidClassException(cls);
		}
		return str;
	}


	private static boolean isAbstractedInternal(Class<?> cls) {
		return cls.getSimpleName().contains("Block"); // todo
	}

	private static final int INTERFACE_ADD = ACC_INTERFACE | ACC_ABSTRACT;
	public static final Abstracter<ClassNode> INTERFACE_IMPL_ASM = new Abstracter<>(HeaderFunction.ASM,
			ConstructorSupplier.INTERFACE_DEFAULT,
			FieldSupplier.INTERFACE_DEFAULT,
			MethodSupplier.INTERFACE_DEFAULT,
			InterfaceFunction.INTERFACE_DEFAULT,
			SuperFunction.INTERFACE_DEFAULT,
			ToStringFunction.INTERFACE_DEFAULT,
			i -> (i & (~ACC_ENUM)) | INTERFACE_ADD,
			FieldAbstracter.INTERFACE_IMPL_ASM,
			MethodAbstracter.INTERFACE_IMPL_ASM,
			ConstructorAbstracter.INTERFACE_IMPL_ASM,
			InnerClassVisit.ASM,
			SerializingFunction.ASM);
	public static final Abstracter<ClassNode> INTERFACE_API_ASM = new Abstracter<>(
			HeaderFunction.ASM,
			ConstructorSupplier.INTERFACE_DEFAULT,
			FieldSupplier.INTERFACE_DEFAULT,
			MethodSupplier.INTERFACE_DEFAULT,
			InterfaceFunction.INTERFACE_DEFAULT,
			SuperFunction.INTERFACE_DEFAULT,
			ToStringFunction.INTERFACE_DEFAULT,
			i -> (i & (~ACC_ENUM)) | INTERFACE_ADD,
			FieldAbstracter.INTERFACE_API_ASM,
			MethodAbstracter.INTERFACE_API_ASM,
			ConstructorAbstracter.INTERFACE_API_ASM,
			InnerClassVisit.ASM,
			SerializingFunction.ASM
	);
	
	public static final Abstracter<ClassNode> BASE_IMPL_ASM = new Abstracter<>(
			HeaderFunction.ASM,
			ConstructorSupplier.BASE_DEFAULT,
			FieldSupplier.BASE_DEFAULT,
			MethodSupplier.BASE_DEFAULT,
			InterfaceFunction.BASE_DEFAULT,
			SuperFunction.BASE_IMPL_DEFAULT,
			ToStringFunction.BASE_DEFAULT,
			IntUnaryOperator.identity(),
			FieldAbstracter.BASE_IMPL_ASM,
			MethodAbstracter.BASE_IMPL_ASM,
			ConstructorAbstracter.BASE_IMPL_ASM,
			InnerClassVisit.ASM,
			SerializingFunction.ASM
	);
	public static final Abstracter<ClassNode> BASE_API_ASM = new Abstracter<>(
			HeaderFunction.ASM,
			ConstructorSupplier.BASE_DEFAULT,
			FieldSupplier.BASE_DEFAULT,
			MethodSupplier.BASE_DEFAULT,
			InterfaceFunction.BASE_DEFAULT,
			SuperFunction.BASE_API_DEFAULT,
			ToStringFunction.BASE_DEFAULT,
			IntUnaryOperator.identity(),
			FieldAbstracter.BASE_API_ASM,
			MethodAbstracter.BASE_API_ASM,
			ConstructorAbstracter.BASE_API_ASM,
			InnerClassVisit.ASM,
			SerializingFunction.ASM
	);

	public static final Abstracter<TypeSpec.Builder> INTERFACE_API_JAVA = new Abstracter<>(
			HeaderFunction.JAVA,
			ConstructorSupplier.INTERFACE_DEFAULT,
			FieldSupplier.INTERFACE_DEFAULT,
			MethodSupplier.INTERFACE_DEFAULT,
			InterfaceFunction.INTERFACE_DEFAULT,
			SuperFunction.INTERFACE_DEFAULT,
			ToStringFunction.INTERFACE_DEFAULT,
			IntUnaryOperator.identity(),
			FieldAbstracter.INTERFACE_API_JAVA,
			MethodAbstracter.API_JAVA,
			ConstructorAbstracter.INTERFACE_API_JAVA,
			InnerClassVisit.JAVA,
			SerializingFunction.JAVA
	);
	
	public static final Abstracter<TypeSpec.Builder> BASE_API_JAVA = new Abstracter<>(
			HeaderFunction.JAVA,
			ConstructorSupplier.BASE_DEFAULT,
			FieldSupplier.BASE_DEFAULT,
			MethodSupplier.BASE_DEFAULT,
			InterfaceFunction.BASE_DEFAULT,
			SuperFunction.BASE_API_DEFAULT,
			ToStringFunction.BASE_DEFAULT,
			IntUnaryOperator.identity(),
			FieldAbstracter.BASE_API_JAVA,
			MethodAbstracter.API_JAVA,
			ConstructorAbstracter.BASE_API_JAVA,
			InnerClassVisit.JAVA,
			SerializingFunction.JAVA
	);

	
	
	
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
	protected final InnerClassVisit<T> innerClassVisitor;
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
			InnerClassVisit<T> visitor,
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
		this.innerClassVisitor = visitor;
		this.serializer = serializer;
	}

	public T apply(ZipOutputStream out, Class<?> cls) {
		T header = this.headerFunction.createHeader(this.accessOperator.applyAsInt(cls.getModifiers()),
				this.nameFunction.toString(cls),
				cls.getTypeParameters(),
				this.superFunction.findValidSuper(cls),
				this.interfaceFunction.getInterfaces(cls));

		for (Field field : this.fieldSupplier.getFields(cls)) {
			this.fieldAbstracter.abstractField(header, cls, field);
		}

		for (Constructor<?> constructor : this.constructorSupplier.getConstructors(cls)) {
			this.constructorAbstracter.abstractConstructor(header, cls, constructor);
		}

		for (Method method : this.methodSupplier.getMethods(cls)) {
			this.methodAbstracter.abstractMethod(header, cls, method);
		}

		return header;
	}

	public void visitInnerClass(T currentHeader, Class<?> currentClass, T innerHeader, Class<?> innerClass) {
		this.innerClassVisitor.visitInnerClass(currentHeader, currentClass, innerHeader, innerClass);
	}

	public void write(ZipOutputStream stream, Class<?> original, T header) {
		try {
			this.serializer.serialize(stream, original, header);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
