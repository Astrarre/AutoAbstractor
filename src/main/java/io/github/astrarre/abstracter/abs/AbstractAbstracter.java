package io.github.astrarre.abstracter.abs;

import static org.objectweb.asm.Type.getInternalName;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.common.reflect.TypeToken;
import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.AbstracterUtil;
import io.github.astrarre.abstracter.abs.field.FieldAbstracter;
import io.github.astrarre.abstracter.abs.member.MemberAbstracter;
import io.github.astrarre.abstracter.abs.method.MethodAbstracter;
import io.github.astrarre.abstracter.func.elements.ConstructorSupplier;
import io.github.astrarre.abstracter.func.elements.FieldSupplier;
import io.github.astrarre.abstracter.func.elements.MethodSupplier;
import io.github.astrarre.abstracter.func.inheritance.InterfaceFunction;
import io.github.astrarre.abstracter.func.inheritance.SuperFunction;
import io.github.astrarre.abstracter.func.map.TypeMappingFunction;
import io.github.astrarre.abstracter.func.post.AttachPostProcessor;
import io.github.astrarre.abstracter.func.post.ExtensionMethodPostProcessor;
import io.github.astrarre.abstracter.func.post.PostProcessor;
import io.github.astrarre.abstracter.util.AnnotationReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

/**
 * an abstract class for abstracting a class, this contains shell logic
 */
@SuppressWarnings ("UnstableApiUsage")
public abstract class AbstractAbstracter implements Opcodes {
	public static final Remapper REMAPPER = new Remapper() {
		@Override
		public String map(String internalName) {
			Class<?> cls = AbstracterConfig.getClass(internalName);
			return AbstracterConfig.getInterfaceName(cls);
		}
	};
	private static final String RUNTIME_EXCEPTION = getInternalName(RuntimeException.class);
	public final Class<?> cls;
	public String name;
	protected InterfaceFunction interfaces;
	protected SuperFunction superFunction;
	protected ConstructorSupplier constructorSupplier;
	protected FieldSupplier fieldSupplier;
	protected MethodSupplier methodSupplier;
	protected PostProcessor processor;
	private AbstractAbstracter outer;
	private List<AbstractAbstracter> innerClasses = new ArrayList<>();

	protected AbstractAbstracter(Class<?> cls,
			String name,
			InterfaceFunction interfaces,
			SuperFunction function,
			ConstructorSupplier supplier,
			FieldSupplier fieldSupplier,
			MethodSupplier methodSupplier) {
		this.cls = AbstracterConfig.getClass(org.objectweb.asm.Type.getInternalName(cls));
		this.name = name;
		this.interfaces = interfaces;
		this.superFunction = function;
		this.constructorSupplier = supplier;
		this.fieldSupplier = fieldSupplier;
		this.methodSupplier = methodSupplier;
	}

	public static String getName(Class<?> cls, String prefix, int version) {
		String str = getInternalName(cls);
		str = str.replace("net/minecraft/", String.format(AbstracterUtil.pkg, version));
		int last = str.lastIndexOf('/') + 1;
		return str.substring(0, last) + prefix + str.substring(last);
	}

	public static boolean conflicts(String name, String desc, ClassNode node) {
		for (MethodNode method : node.methods) {
			if (name.equals(method.name) && desc.equals(method.desc)) {
				return true;
			}
		}
		return false;
	}

	public static void visitStub(MethodNode visitor) {
		if (!Modifier.isAbstract(visitor.access)) {
			visitor.visitTypeInsn(NEW, RUNTIME_EXCEPTION);
			visitor.visitInsn(DUP);
			visitor.visitMethodInsn(INVOKESPECIAL, RUNTIME_EXCEPTION, "<init>", "()V", false);
			visitor.visitInsn(ATHROW);
		}
	}

	public String getInterfaceDesc(Class<?> cls) {
		if (cls.isPrimitive()) {
			return org.objectweb.asm.Type.getDescriptor(cls);
		} else if (cls.isArray()) {
			return '[' + this.getInterfaceDesc(cls.getComponentType());
		} else {
			return "L" + AbstracterConfig.getInterfaceName(cls) + ";";
		}
	}

	/**
	 * Create the abstracted classnode
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
			header.interfaces.add(AbstracterConfig.getInterfaceName(MemberAbstracter.raw(iface)));
		}

		Type sup = this.superFunction.findValidSuper(this.cls, impl);
		if (sup != null) {
			header.superName = MemberAbstracter.getRawName(sup);
		}

		header.signature = MemberAbstracter.classSignature(this.cls.getTypeParameters(), sup, interfaces);

		this.preProcess(header);
		for (Constructor<?> constructor : this.constructorSupplier.getConstructors(this.cls)) {
			MethodAbstracter<Constructor<?>> abstracter = this.abstractConstructor(constructor, impl);
			if (abstracter != null) {
				abstracter.abstractMethod(header);
			}
		}

		for (Method method : this.methodSupplier.getMethods(this.cls)) {
			MethodAbstracter<Method> abstracter = this.abstractMethod(method, impl);
			if (abstracter != null) {
				abstracter.abstractMethod(header);
			}
		}

		for (Field field : this.fieldSupplier.getFields(this.cls)) {
			FieldAbstracter abstracter = this.abstractField(field, impl);
			if (abstracter != null) {
				abstracter.abstractField(header);
			}
		}

		for (AbstractAbstracter abstracter : this.innerClasses) {
			String name = abstracter.name;
			int split = name.lastIndexOf('$');
			if (split == -1) {
				throw new IllegalArgumentException(abstracter.name + " does not have $, and cannot be an inner class!");
			}
			header.visitInnerClass(name, name.substring(0, split), name.substring(split + 1), abstracter.getAccess(abstracter.cls.getModifiers()));
		}

		if (this.outer != null) {
			header.visitOuterClass(this.outer.name, null, null);
		}

		this.postProcess(header, impl);
		return header;
	}

	/**
	 * @return get a class's access flags
	 */
	public abstract int getAccess(int modifiers);

	protected void preProcess(ClassNode node) {
		MethodNode init = new MethodNode(ACC_STATIC | ACC_PUBLIC, "astrarre_artificial_clinit", "()V", null, null);
		node.methods.add(init);
	}

	public abstract MethodAbstracter<Constructor<?>> abstractConstructor(Constructor<?> constructor, boolean impl);

	public abstract MethodAbstracter<Method> abstractMethod(Method method, boolean impl);

	public abstract FieldAbstracter abstractField(Field field, boolean impl);

	protected void postProcess(ClassNode node, boolean impl) {
		if (this.processor != null) {
			this.processor.process(this.cls, node, impl);
		}

		Iterator<MethodNode> iterator = node.methods.iterator();
		while (iterator.hasNext()) {
			MethodNode method = iterator.next();
			if ("astrarre_artificial_clinit".equals(method.name)) {
				if (method.instructions.size() > 0) {
					method.visitInsn(RETURN);
				} else {
					iterator.remove();
				}
				return;
			}
		}
	}

	/**
	 * cast the current type to it's minecraft type
	 *
	 * @param visitor the method to visit the instructions
	 * @param apply calling this function will put the desired value on the stack
	 * @param parameter if true, `apply` gets it's value from a parameter
	 */
	public abstract void castToMinecraft(MethodVisitor visitor, Consumer<MethodVisitor> apply, Location parameter);

	/**
	 * cast the this minecraft type to it's type
	 *
	 * @param visitor the method to visit the instructions
	 * @param apply calling this function will put the desired value on the stack
	 */
	public abstract void castToCurrent(MethodVisitor visitor, Consumer<MethodVisitor> apply, Location parameter);

	public String getDesc(Location location) {
		return 'L' + this.name + ';';
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

	public AbstractAbstracter filterMethod(String name, String desc) {
		this.methodSupplier = this.methodSupplier.filtered((abstracting, method) -> method.getName()
		                                                                                  .equals(name) && org.objectweb.asm.Type.getMethodDescriptor(
				method).equals(desc));
		return this;
	}

	public AbstractAbstracter filterMethod(String name) {
		this.methodSupplier = this.methodSupplier.filtered((abstracting, method) -> method.getName().equals(name));
		return this;
	}

	/**
	 * attaches an extension method to the class in post-process the method must be refered to by a method reference, and must be static
	 */
	public <A> AbstractAbstracter extension(SConsumer<A> consumer) {
		return this.extension(ExtensionMethodPostProcessor.reverseReference(consumer));
	}

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

	public <A, B> AbstractAbstracter extension(SBiConsumer<A, B> consumer) {
		return this.extension(ExtensionMethodPostProcessor.reverseReference(consumer));
	}

	public <A, B, C> AbstractAbstracter extension(STriConsumer<A, B, C> consumer) {
		return this.extension(ExtensionMethodPostProcessor.reverseReference(consumer));
	}

	public <A, B, C, D> AbstractAbstracter extension(SQuadConsumer<A, B, C, D> consumer) {
		return this.extension(ExtensionMethodPostProcessor.reverseReference(consumer));
	}

	public <A, B, C, D, E> AbstractAbstracter extension(SPentaConsumer<A, B, C, D, E> consumer) {
		return this.extension(ExtensionMethodPostProcessor.reverseReference(consumer));
	}

	public AbstractAbstracter extension(Serializable consumer) {return this.extension(ExtensionMethodPostProcessor.reverseReference(consumer));}

	public AbstractAbstracter attach(TypeToken<?> token) {return this.attach(token.getType());}

	public AbstractAbstracter attach(Type type) {return this.post(new AttachPostProcessor(type));}

	/**
	 * attaches an interface to the class in post-process. The interface signature and class is the first class the passed class implements, this is
	 * useful for making interfaces that use the attached class's generic variables.
	 */
	public AbstractAbstracter attachFirstInterface(Class<?> cls) {return this.attach(cls.getGenericInterfaces()[0]);}

	public AbstractAbstracter addInner(AbstractAbstracter abstracter) {
		if (abstracter.outer != null) {
			throw new IllegalArgumentException("abstracter already has outer class");
		} else abstracter.outer = this;
		this.innerClasses.add(abstracter);
		return this;
	}

	public static MethodNode findMethod(ClassNode node, String name, String desc) {
		for (MethodNode method : node.methods) {
			if (name.equals(method.name) && desc.equals(method.desc)) {
				return method;
			}
		}
		throw new IllegalArgumentException("unable to find " + name + desc + " in " + node.name);
	}

	public enum Location {
		THIS, PARAMETER, RETURN
	}

	// @formatter:off
	public interface SConsumer<T> extends Consumer<T>, Serializable {}
	public interface SBiConsumer<T, V> extends BiConsumer<T, V>, Serializable {}
	public interface STriConsumer<A, B, C> extends Serializable {void accept(A a, B b, C c);}
	public interface SQuadConsumer<A, B, C, D> extends Serializable {void accept(A a, B b, C c, D d);}
	public interface SPentaConsumer<A, B, C, D, E> extends Serializable {void accept(A a, B b, C c, D d, E e);}
}
