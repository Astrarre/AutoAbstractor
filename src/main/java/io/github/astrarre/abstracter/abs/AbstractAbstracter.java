package io.github.astrarre.abstracter.abs;

import static org.objectweb.asm.Type.getInternalName;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.AbstracterUtil;
import io.github.astrarre.abstracter.abs.field.FieldAbstracter;
import io.github.astrarre.abstracter.abs.method.MethodAbstracter;
import io.github.astrarre.abstracter.func.elements.ConstructorSupplier;
import io.github.astrarre.abstracter.func.elements.FieldSupplier;
import io.github.astrarre.abstracter.func.elements.MethodSupplier;
import io.github.astrarre.abstracter.func.inheritance.InterfaceFunction;
import io.github.astrarre.abstracter.func.inheritance.SuperFunction;
import io.github.astrarre.abstracter.func.post.PostProcessor;
import io.github.astrarre.abstracter.util.AnnotationReader;
import io.github.astrarre.abstracter.util.AsmUtil;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * an abstract class for abstracting a class, this contains shell logic
 */
@SuppressWarnings ("UnstableApiUsage")
public abstract class AbstractAbstracter implements Opcodes {
	public final String cls;
	public String name;
	protected InterfaceFunction interfaces;
	protected SuperFunction superFunction;
	protected ConstructorSupplier constructorSupplier;
	protected FieldSupplier fieldSupplier;
	protected MethodSupplier methodSupplier;
	protected PostProcessor processor;
	private AbstractAbstracter outer;
	private List<AbstractAbstracter> innerClasses = new ArrayList<>();
	private AbstracterConfig last;
	private Class<?> cached;

	protected AbstractAbstracter(Class<?> cls,
			String name,
			InterfaceFunction interfaces,
			SuperFunction function,
			ConstructorSupplier supplier,
			FieldSupplier fieldSupplier,
			MethodSupplier methodSupplier) {
		this.cls = org.objectweb.asm.Type.getInternalName(cls);
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

	/**
	 * Create the abstracted classnode
	 */
	public ClassNode apply(AbstracterConfig config, boolean impl) {
		ClassNode header = new ClassNode();
		header.version = V1_8;
		header.access = this.getAccess(config, this.getCls(config).getModifiers());
		header.name = this.name;
		for (Annotation annotation : this.getCls(config).getAnnotations()) {
			if (header.visibleAnnotations == null) {
				header.visibleAnnotations = new ArrayList<>();
			}
			header.visibleAnnotations.add(AnnotationReader.accept(annotation));
		}

		Collection<Type> interfaces = this.interfaces.getInterfaces(config, this.getCls(config));
		for (Type iface : interfaces) {
			header.interfaces.add(config.getInterfaceName(AsmUtil.raw(iface)));
		}

		Type sup = this.superFunction.findValidSuper(config, this.getCls(config), impl);
		header.superName = getInternalName(AsmUtil.raw(sup));

		TypeVariable<?>[] variables = this.getCls(config).getTypeParameters();
		SignatureWriter writer = new SignatureWriter();
		if (!(variables.length == 0 && sup instanceof Class && interfaces.stream().allMatch(t -> t instanceof Class))) {
			AsmUtil.visit(config, writer, variables);
			AsmUtil.visit(config, writer.visitSuperclass(), sup);
			for (Type iface : interfaces) {
				AsmUtil.visit(config, writer.visitInterface(), iface);
			}
			header.signature = writer.toString();
		}


		this.preProcess(header);
		for (Constructor<?> constructor : this.constructorSupplier.getConstructors(config, this.getCls(config))) {
			MethodAbstracter<Constructor<?>> abstracter = this.abstractConstructor(config, constructor, impl);
			if (abstracter != null) {
				abstracter.abstractMethod(header);
			}
		}

		for (Method method : this.methodSupplier.getMethods(config, this.getCls(config))) {
			MethodAbstracter<Method> abstracter = this.abstractMethod(config, method, impl);
			if (abstracter != null) {
				abstracter.abstractMethod(header);
			}
		}

		for (Field field : this.fieldSupplier.getFields(config, this.getCls(config))) {
			FieldAbstracter abstracter = this.abstractField(config, field, impl);
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
			header.visitInnerClass(name,
					name.substring(0, split),
					name.substring(split + 1),
					abstracter.getAccess(config, abstracter.getCls(config).getModifiers()));
		}

		if (this.outer != null) {
			header.visitOuterClass(this.outer.name, null, null);
		}

		this.postProcess(config, header, impl);
		return header;
	}

	/**
	 * @return get a class's access flags
	 */
	public abstract int getAccess(AbstracterConfig config, int modifiers);

	public Class<?> getCls(AbstracterConfig config) {
		if (this.last != config) {
			this.last = config;
			return this.cached = config.getClass(this.cls);
		}
		return this.cached;
	}

	protected void preProcess(ClassNode node) {
		MethodNode init = new MethodNode(ACC_STATIC | ACC_PUBLIC, "astrarre_artificial_clinit", "()V", null, null);
		node.methods.add(init);
	}

	public abstract MethodAbstracter<Constructor<?>> abstractConstructor(AbstracterConfig config, Constructor<?> constructor, boolean impl);

	public abstract MethodAbstracter<Method> abstractMethod(AbstracterConfig config, Method method, boolean impl);

	public abstract FieldAbstracter abstractField(AbstracterConfig config, Field field, boolean impl);

	protected void postProcess(AbstracterConfig config, ClassNode node, boolean impl) {
		if (this.processor != null) {
			this.processor.process(config, this.getCls(config), node, impl);
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

	/**
	 * do not visit end
	 * @return true if the type parameters should be visited
	 */
	public boolean visitSign(Location location, SignatureVisitor visitor) {
		String desc = this.getDesc(location);
		visitor.visitClassType(desc.substring(1, desc.length()-1));
		return true;
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
		this.methodSupplier = this.methodSupplier.filtered((config, abstracting, method) -> method.getName()
		                                                                                          .equals(name) && org.objectweb.asm.Type.getMethodDescriptor(
				method).equals(desc));
		return this;
	}

	public AbstractAbstracter filterMethod(String name) {
		this.methodSupplier = this.methodSupplier.filtered((config, abstracting, method) -> method.getName().equals(name));
		return this;
	}

	public AbstractAbstracter post(PostProcessor processor) {
		if (this.processor == null) {
			this.processor = processor;
		} else {
			this.processor = this.processor.andThen(processor);
		}
		return this;
	}

	public AbstractAbstracter addInner(AbstractAbstracter abstracter) {
		if (abstracter.outer != null) {
			throw new IllegalArgumentException("abstracter already has outer class");
		} else {
			abstracter.outer = this;
		}
		this.innerClasses.add(abstracter);
		return this;
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
