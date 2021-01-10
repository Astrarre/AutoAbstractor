package io.github.astrarre.abstracter.abs;

import static org.objectweb.asm.Type.getInternalName;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.common.reflect.TypeToken;
import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.AbstracterUtil;
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
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
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

	public static String getEtterName(String prefix, String name) {
		return prefix + Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	public static String toSignature(Type reified) {
		SignatureWriter writer = new SignatureWriter();
		visit(writer, reified);
		return writer.toString();
	}

	public static String classSignature(TypeVariable<?>[] variables, Type superClass, Collection<Type> interfaces) {
		SignatureWriter writer = new SignatureWriter();
		if (variables.length == 0 && (superClass instanceof Class || superClass == null) && interfaces.stream().allMatch(t -> t instanceof Class)) {
			return null;
		}

		visit(writer, variables);
		visit(writer.visitSuperclass(), superClass);
		for (Type iface : interfaces) {
			visit(writer.visitInterface(), iface);
		}
		return writer.toString();
	}

	public static void visit(SignatureVisitor visitor, TypeVariable<?>[] variables) {
		for (TypeVariable<?> variable : variables) {
			visit(visitor, variable);
		}
	}

	public static void visit(SignatureVisitor visitor, TypeVariable<?> variable) {
		visitor.visitFormalTypeParameter(variable.getName());
		boolean first = true;
		for (Type bound : variable.getBounds()) {
			if (first) {
				visitor.visitClassBound();
				first = false;
			} else {
				visitor.visitInterfaceBound();
			}
			visit(visitor, bound);
		}
	}

	public static void visit(SignatureVisitor visitor, Type type, boolean remap) {
		visit(visitor, type, remap, true);
	}

	public static void visit(SignatureVisitor visitor, Type type, boolean remap, boolean visitEnd) {
		if (type instanceof Class<?>) {
			Class<?> c = (Class<?>) type;
			if (c.isArray()) {
				visit(visitor.visitArrayType(), c.getComponentType(), remap);
			} else if (c.isPrimitive()) {
				visitor.visitBaseType(org.objectweb.asm.Type.getDescriptor(c).charAt(0));
			} else {
				if (remap) {
					visitor.visitClassType(AbstracterConfig.getInterfaceName(c));
				} else {
					visitor.visitClassType(getInternalName(c));
				}
				if (visitEnd) {
					visitor.visitEnd();
				}
			}
			return;
		} else if (type instanceof GenericArrayType) {
			visit(visitor.visitArrayType(), ((GenericArrayType) type).getGenericComponentType(), remap);
			return;
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			Type owner = pt.getOwnerType();
			Class<?> raw = (Class<?>) pt.getRawType();
			if (owner != null) {
				visit(visitor, owner, remap, false);
				visitor.visitInnerClassType(raw.getSimpleName());
			} else {
				// visit the type
				if (raw.isPrimitive()) {
					visitor.visitBaseType(org.objectweb.asm.Type.getDescriptor(raw).charAt(0));
				} else {
					visitor.visitClassType(getInternalName(raw));
				}
			}

			Type[] args = pt.getActualTypeArguments();
			for (Type arg : args) {
				if (!(arg instanceof WildcardType)) {
					visitor.visitTypeArgument('=');
				}
				visit(visitor, arg, remap);
			}

			if (visitEnd) {
				visitor.visitEnd();
			}
			return;
		} else if (type instanceof TypeVariable<?>) {
			visitor.visitTypeVariable(((TypeVariable<?>) type).getName());
			return;
		} else if (type instanceof WildcardType) {
			WildcardType wt = (WildcardType) type;
			Type[] array = wt.getLowerBounds();
			if (array.length > 0) {
				visitor.visitTypeArgument('-');
			} else {
				array = wt.getUpperBounds();
				if (array.length == 1 && array[0] == Object.class) {
					visitor.visitTypeArgument();
				} else {
					visitor.visitTypeArgument('+');
				}
			}

			for (Type l : array) {
				visit(visitor, l, remap);
			}
			return;
		} else if (type == null) {
			return;
		}
		throw new IllegalArgumentException("Unrecognized type " + type + " " + type.getClass());
	}

	public static void visit(SignatureVisitor visitor, Type type) {
		visit(visitor, type, true);
	}

	public static StringBuilder typeVarsAsString(TypeVariable<?>[] variables) {
		if (variables.length > 0) {
			StringBuilder builder = new StringBuilder();
			builder.append('<');
			for (TypeVariable<?> variable : variables) {
				builder.append(variable.getName());
				for (Type bound : variable.getBounds()) {
					builder.append(':').append(toSignature(bound));
				}
			}
			builder.append('>');
			return builder;
		}
		return new StringBuilder();
	}

	public static String methodSignature(TypeVariable<?>[] variables, TypeToken<?>[] parameters, TypeToken<?> returnType) {
		StringBuilder builder = typeVarsAsString(variables);
		builder.append('(');
		for (TypeToken<?> parameter : parameters) {
			builder.append(toSignature(parameter.getType()));
		}
		builder.append(')');
		builder.append(toSignature(returnType.getType()));
		return builder.toString();
	}

	public static String methodDescriptor(TypeToken<?>[] parameters, TypeToken<?> returnType) {
		StringBuilder builder = new StringBuilder();
		builder.append('(');
		for (TypeToken<?> parameter : parameters) {
			builder.append(toSignature(parameter.getRawType()));
		}
		builder.append(')');
		builder.append(toSignature(returnType.getRawType()));
		return builder.toString();
	}

	public static String getRawName(Type type) {
		return getInternalName(raw(type));
	}

	public static String getInterfaceDesc(Class<?> cls) {
		if (cls.isPrimitive()) {
			return org.objectweb.asm.Type.getDescriptor(cls);
		} else if (cls.isArray()) {
			return '[' + getInterfaceDesc(cls.getComponentType());
		} else {
			return "L" + AbstracterConfig.getInterfaceName(cls) + ";";
		}
	}

	public static Class<?> raw(Type type) {
		if (type instanceof Class<?>) {
			return (Class<?>) type;
		} else if (type instanceof GenericArrayType) {
			return Array.newInstance(raw(((GenericArrayType) type).getGenericComponentType()), 0).getClass();
		} else if (type instanceof ParameterizedType) {
			return (Class<?>) ((ParameterizedType) type).getRawType();
		} else if (type instanceof TypeVariable<?>) {
			Iterator<Type> iterator = Arrays.asList(((TypeVariable<?>) type).getBounds()).iterator();
			while (iterator.hasNext()) {
				Type bound = iterator.next();
				if (bound != Object.class) {
					return raw(bound);
				} else if (!iterator.hasNext()) {
					return Object.class;
				}
			}
		} else if (type instanceof WildcardType) {
			// todo
		} else if (type == null) {
			return Object.class;
		}
		throw new UnsupportedOperationException("Raw type " + type + " not found!");
	}

	public static String getInnerName(String str) {
		int index = str.indexOf('$');
		return str.substring(index + 1);
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
			header.interfaces.add(AbstracterConfig.getInterfaceName(raw(iface)));
		}

		Type sup = this.superFunction.findValidSuper(this.cls, impl);
		if (sup != null) {
			header.superName = getRawName(sup);
		}

		header.signature = classSignature(this.cls.getTypeParameters(), sup, interfaces);

		this.preProcess(header, impl);
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
			this.abstractField(header, field, impl);
		}

		this.postProcess(header, impl);
		return header;
	}

	/**
	 * @return get a class's access flags
	 */
	public abstract int getAccess(int modifiers);

	protected void preProcess(ClassNode node, boolean impl) {
		if (impl) {
			MethodNode init = new MethodNode(ACC_STATIC | ACC_PUBLIC, "astrarre_artificial_clinit", "()V", null, null);
			node.methods.add(init);
		}
	}

	public abstract MethodAbstracter<Constructor<?>> abstractConstructor(Constructor<?> constructor, boolean impl);

	public abstract MethodAbstracter<Method> abstractMethod(Method method, boolean impl);

	public abstract void abstractField(ClassNode node, Field field, boolean impl);

	protected void postProcess(ClassNode node, boolean impl) {
		if (this.processor != null) {
			this.processor.process(this.cls, node, impl);
		}

		if (impl) {
			for (MethodNode method : node.methods) {
				if ("astrarre_artificial_clinit".equals(method.name)) {
					method.visitInsn(RETURN);
					return;
				}
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

	// todo annotations
	public MethodNode createGetter(String abstractedName, Class<?> cls, Field field, boolean impl, boolean iface) {
		int access = field.getModifiers();
		access &= ~ACC_ENUM;
		String owner = getInternalName(field.getDeclaringClass());
		TypeToken<?> token = TypeToken.of(cls).resolveType(field.getGenericType());
		String descriptor = getInterfaceDesc(token.getRawType());
		String name = field.getName();
		String signature = toSignature(token.getType());
		MethodNode node = new MethodNode(access,
				getEtterName("get", name),
				"()" + descriptor,
				signature.equals(descriptor) ? null : "()" + signature,
				null);
		for (Annotation annotation : field.getAnnotations()) {
			if (node.visibleAnnotations == null) {
				node.visibleAnnotations = new ArrayList<>();
			}
			node.visibleAnnotations.add(AnnotationReader.accept(annotation));
		}
		if (impl) {
			org.objectweb.asm.Type ret = org.objectweb.asm.Type.getType(field.getType());
			if (Modifier.isStatic(access)) {
				node.visitFieldInsn(GETSTATIC, owner, name, ret.getDescriptor());
			} else {
				node.visitVarInsn(ALOAD, 0);
				if (iface) {
					// fixme: cast(abstractedName, owner, node);
				}

				node.visitFieldInsn(GETFIELD, owner, name, ret.getDescriptor());
			}

			if (!ret.getDescriptor().equals(descriptor)) {
				// fixme: cast(ret.getInternalName(), org.objectweb.asm.Type.getType(descriptor).getInternalName(), node);
			}
			node.visitInsn(org.objectweb.asm.Type.getType(descriptor).getOpcode(IRETURN));
		} else {
			this.visitStub(node);
		}

		return node;
	}

	public MethodNode createSetter(String abstractedName, Class<?> cls, Field field, boolean impl, boolean iface) {
		int access = field.getModifiers();
		access &= ~ACC_ENUM;
		String owner = getInternalName(field.getDeclaringClass());
		TypeToken<?> token = TypeToken.of(cls).resolveType(field.getGenericType());
		String descriptor = getInterfaceDesc(token.getRawType());
		String name = field.getName();
		String signature = toSignature(token.getType());
		MethodNode node = new MethodNode(access,
				getEtterName("set", name),
				"(" + descriptor + ")V",
				signature.equals(descriptor) ? null : "(" + signature + ")V",
				null);
		for (Annotation annotation : field.getAnnotations()) {
			if (node.visibleAnnotations == null) {
				node.visibleAnnotations = new ArrayList<>();
			}
			node.visibleAnnotations.add(AnnotationReader.accept(annotation));
		}
		org.objectweb.asm.Type type = org.objectweb.asm.Type.getType(descriptor);
		if (impl) {
			if (Modifier.isStatic(access)) {
				node.visitVarInsn(type.getOpcode(ILOAD), 0);
				node.visitFieldInsn(PUTSTATIC, owner, name, descriptor);
			} else {
				node.visitVarInsn(ALOAD, 0);
				if (iface) {
					// fixme: cast(abstractedName, owner, node);
				}
				node.visitVarInsn(type.getOpcode(ILOAD), 1);
				if (!org.objectweb.asm.Type.getDescriptor(field.getType()).equals(descriptor)) {
					// fixme: cast(type.getInternalName(), getInternalName(field.getType()), node);
				}
				node.visitFieldInsn(PUTFIELD, owner, name, org.objectweb.asm.Type.getDescriptor(field.getType()));
			}
		} else {
			AbstractAbstracter.visitStub(node);
		}
		node.visitInsn(RETURN);
		node.visitParameter(name, 0);
		return node;
	}

	public void createConstant(ClassNode header, Class<?> cls, Field field, boolean impl) {
		Type reified = TypeMappingFunction.reify(cls, field.getGenericType());
		FieldNode node = new FieldNode(field.getModifiers() & ~ACC_ENUM,
				field.getName(),
				getInterfaceDesc(TypeMappingFunction.raw(cls, field.getGenericType())),
				toSignature(reified),
				null);

		// these actually exist
		if (Modifier.isStatic(node.access)) {
			MethodNode init = this.findOrCreateMethod(ACC_STATIC | ACC_PUBLIC, header, "astrarre_artificial_clinit", "()V");
			InsnList list = init.instructions;
			if (list.getLast() == null) {
				list.insert(new InsnNode(RETURN));
			}

			if (impl) {
				InsnList insn = new InsnList();
				insn.add(new FieldInsnNode(GETSTATIC,
						getInternalName(field.getDeclaringClass()),
						field.getName(),
						org.objectweb.asm.Type.getDescriptor(field.getType())));
				insn.add(new FieldInsnNode(PUTSTATIC, header.name, node.name, node.desc));
				list.insert(insn);
			}
		}
		header.fields.add(node);
	}

	public MethodNode findOrCreateMethod(int access, ClassNode node, String name, String desc) {
		for (MethodNode method : node.methods) {
			if (name.equals(method.name) && desc.equals(method.desc)) {
				return method;
			}
		}
		MethodNode method = new MethodNode(access, name, desc, null, null);
		node.methods.add(method);
		return method;
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
