package io.github.astrarre.abstracter.abs;

import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.common.reflect.TypeToken;
import io.github.astrarre.FieldRef;
import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.AbstracterUtil;
import io.github.astrarre.abstracter.func.elements.ConstructorSupplier;
import io.github.astrarre.abstracter.func.elements.FieldSupplier;
import io.github.astrarre.abstracter.func.elements.MethodSupplier;
import io.github.astrarre.abstracter.func.inheritance.InterfaceFunction;
import io.github.astrarre.abstracter.func.inheritance.SuperFunction;
import io.github.astrarre.abstracter.func.post.AttachPostProcessor;
import io.github.astrarre.abstracter.func.post.ExtensionMethodPostProcessor;
import io.github.astrarre.abstracter.util.AbstracterLoader;
import io.github.astrarre.abstracter.util.AnnotationReader;
import io.github.astrarre.abstracter.util.reflect.TypeUtil;
import io.github.astrarre.abstracter.func.post.PostProcessor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * an abstract class for abstracting a class, this contains shell logic
 */
@SuppressWarnings ("UnstableApiUsage")
public abstract class AbstractAbstracter implements Opcodes {
	public static final String FIELD_REF = org.objectweb.asm.Type.getDescriptor(FieldRef.class);
	public static final String FIELD_REF_NAME = getInternalName(FieldRef.class);
	protected final Class<?> cls;
	public String name;
	protected InterfaceFunction interfaces;
	protected SuperFunction superFunction;
	protected ConstructorSupplier constructorSupplier;
	protected FieldSupplier fieldSupplier;
	protected MethodSupplier methodSupplier;
	protected PostProcessor processor;

	protected AbstractAbstracter(Class<?> cls,
			String name,
			InterfaceFunction interfaces,
			SuperFunction function,
			ConstructorSupplier supplier,
			FieldSupplier fieldSupplier,
			MethodSupplier methodSupplier) {
		this.cls = AbstracterLoader.getClass(cls.getName());
		this.name = name;
		this.interfaces = interfaces;
		this.superFunction = function;
		this.constructorSupplier = supplier;
		this.fieldSupplier = fieldSupplier;
		this.methodSupplier = methodSupplier;
	}

	public static String getName(Class<?> cls, String prefix, int version) {
		String str = getInternalName(cls);
		str = str.replace("net/minecraft/", "v" + version + AbstracterUtil.pkg);
		int last = str.lastIndexOf('/') + 1;
		return str.substring(0, last) + prefix + str.substring(last);
	}

	public void addFieldRefAnnotation(MethodVisitor visitor, Field field) {
		AnnotationVisitor visit = visitor.visitAnnotation(FIELD_REF, false);
		visit.visit("owner", getInternalName(field.getDeclaringClass()));
		visit.visit("name", field.getName());
		visit.visit("type", getDescriptor(field.getType()));
		visit.visitEnd();
	}

	/**
	 * Create the abstracted classnode
	 * @param impl true if output abstracted
	 */
	public ClassNode apply(boolean impl) {
		ClassNode header = new ClassNode();
		header.version = V1_8;
		header.access = this.getAccess(this.cls.getModifiers());
		header.name = this.name;
		for (Annotation annotation : this.cls.getAnnotations()) {
			if (header.visibleAnnotations == null) {
				header.visibleAnnotations = new ArrayList<>();
			}
			header.visibleAnnotations.add(AnnotationReader.accept(annotation));
		}

		Collection<Type> interfaces = this.interfaces.getInterfaces(this.cls);
		for (Type iface : interfaces) {
			header.interfaces.add(AbstracterConfig.getInterfaceName(TypeUtil.raw(iface)));
		}

		Type sup = this.superFunction.findValidSuper(this.cls, impl);
		if (sup != null) {
			header.superName = TypeUtil.getRawName(sup);
		}

		header.signature = TypeUtil.classSignature(this.cls.getTypeParameters(), sup, interfaces);

		for (Constructor<?> constructor : this.constructorSupplier.getConstructors(this.cls)) {
			this.abstractConstructor(header, constructor, impl);
		}

		for (Method method : this.methodSupplier.getMethods(this.cls)) {
			this.abstractMethod(header, method, impl);
		}

		for (Field field : this.fieldSupplier.getFields(this.cls)) {
			this.abstractField(header, field, impl);
		}

		this.postProcess(header, impl);
		return header;
	}

	/**
	 * @return get a class's access flags
	 * @param modifiers
	 */
	public abstract int getAccess(int modifiers);

	public abstract void abstractConstructor(ClassNode node, Constructor<?> constructor, boolean impl);

	public abstract void abstractMethod(ClassNode node, Method method, boolean impl);

	public abstract void abstractField(ClassNode node, Field field, boolean impl);

	protected void postProcess(ClassNode node, boolean impl) {
		for (MethodNode method : node.methods) {
			if (method.name.equals("<clinit>")) {
				method.visitInsn(RETURN);
			}
		}
		if (this.processor != null) {
			this.processor.process(this.cls, node, impl);
		}
	}

	public Class<?> getCls() {
		return this.cls;
	}

	public AbstractAbstracter name(String name) {
		this.name = name;
		return this;
	}

	public AbstractAbstracter interfaces(InterfaceFunction interfaces) {
		this.interfaces = interfaces;
		return this;
	}

	public AbstractAbstracter superClass(SuperFunction function) {
		this.superFunction = function;
		return this;
	}

	public AbstractAbstracter constructors(ConstructorSupplier supplier) {
		this.constructorSupplier = supplier;
		return this;
	}

	public AbstractAbstracter fields(FieldSupplier supplier) {
		this.fieldSupplier = supplier;
		return this;
	}

	public AbstractAbstracter methods(MethodSupplier supplier) {
		this.methodSupplier = supplier;
		return this;
	}

	/**
	 * attaches an extension method to the class in post-process
	 * the method must be refered to by a method reference, and must be static
	 */
	public <A> AbstractAbstracter extension(SConsumer<A> consumer) {return this.extension(ExtensionMethodPostProcessor.reverseReference(consumer));}

	public AbstractAbstracter extension(Method method) {
		return this.post(new ExtensionMethodPostProcessor(method));
	}

	public AbstractAbstracter post(PostProcessor processor) {
		if (this.processor == null) {
			this.processor = processor;
		} else {
			this.processor = this.processor.andThen(processor);
		}
		return this;
	}

	public <A, B> AbstractAbstracter extension(SBiConsumer<A, B> consumer) {return this.extension(ExtensionMethodPostProcessor.reverseReference(consumer));}

	public <A, B, C> AbstractAbstracter extension(STriConsumer<A, B, C> consumer) {return this.extension(ExtensionMethodPostProcessor.reverseReference(consumer));}

	public <A, B, C, D> AbstractAbstracter extension(SQuadConsumer<A, B, C, D> consumer) {return this.extension(ExtensionMethodPostProcessor.reverseReference(consumer));}

	public <A, B, C, D, E> AbstractAbstracter extension(SPentaConsumer<A, B, C, D, E> consumer) {return this.extension(ExtensionMethodPostProcessor.reverseReference(consumer));}

	public AbstractAbstracter extension(Serializable consumer) {return this.extension(ExtensionMethodPostProcessor.reverseReference(consumer));}

	public AbstractAbstracter attach(TypeToken<?> token) {return this.attach(token.getType());}

	public AbstractAbstracter attach(Type type) {return this.post(new AttachPostProcessor(type));}

	/**
	 * attaches an interface to the class in post-process.
	 * The interface signature and class is the first class the passed class implements, this is useful for making interfaces that use the
	 * attached class's generic variables.
	 */
	public AbstractAbstracter attachFirstInterface(Class<?> cls) {return this.attach(cls.getGenericInterfaces()[0]);}
	// @formatter:off
	public interface SConsumer<T> extends Consumer<T>, Serializable {}
	public interface SBiConsumer<T, V> extends BiConsumer<T, V>, Serializable {}
	public interface STriConsumer<A, B, C> extends Serializable {void accept(A a, B b, C c);}
	public interface SQuadConsumer<A, B, C, D> extends Serializable {void accept(A a, B b, C c, D d);}

	public interface SPentaConsumer<A, B, C, D, E> extends Serializable {void accept(A a, B b, C c, D d, E e);}
	// @formatter:on
}
