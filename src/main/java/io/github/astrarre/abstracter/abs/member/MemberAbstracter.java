package io.github.astrarre.abstracter.abs.member;

import static org.objectweb.asm.Type.ARRAY;
import static org.objectweb.asm.Type.OBJECT;
import static org.objectweb.asm.Type.getInternalName;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.tree.MethodNode;

public abstract class MemberAbstracter<T extends Member> implements Opcodes {
	protected final AbstractAbstracter abstracter;
	protected final T member;
	protected final boolean impl;

	public MemberAbstracter(AbstractAbstracter abstracter, T member, boolean impl) {
		this.abstracter = abstracter;
		this.member = member;
		this.impl = impl;
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

	public static String getRawName(Type type) {
		return getInternalName(raw(type));
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

	public void cast(AbstractAbstracter.Location location,
			org.objectweb.asm.Type fromType,
			org.objectweb.asm.Type toType,
			MethodNode visitor,
			Consumer<MethodVisitor> apply) {
		if (fromType.equals(toType)) {
			apply.accept(visitor);
			return;
		}

		if (fromType.getSort() == OBJECT) {
			String internalName = fromType.getInternalName();
			AbstractAbstracter abstracter = AbstracterConfig.getInterfaceAbstraction(internalName);
			if (abstracter != null) {
				if (toType.getDescriptor().equals(abstracter.getDesc(location))) {
					abstracter.castToMinecraft(visitor, apply, location);
					return;
				} else {
					throw new IllegalStateException(toType + " --/--> " + abstracter.getDesc(location));
				}
			}
		}

		if (toType.getSort() == OBJECT) {
			AbstractAbstracter abstracter = AbstracterConfig.getInterfaceAbstraction(toType.getInternalName());
			if (abstracter != null) {
				if (fromType.getDescriptor().equals(abstracter.getDesc(location))) {
					abstracter.castToCurrent(visitor, apply, location);
					return;
				} else {
					throw new IllegalStateException(toType + " --/--> " + abstracter.getDesc(location));
				}
			}
		}

		if ((toType.getSort() & fromType.getSort()) == OBJECT) {
			Class<?> from = AbstracterConfig.getClass(fromType.getInternalName()), to = AbstracterConfig.getClass(toType.getInternalName());
			if (!to.isAssignableFrom(from)) {
				apply.accept(visitor);
				visitor.visitTypeInsn(CHECKCAST, org.objectweb.asm.Type.getInternalName(to));
			}
		}

		if(fromType.getSort() == ARRAY && toType.getSort() == ARRAY) {
			try {
				String name = fromType.getInternalName();
				AbstracterConfig.getClass(name);
				visitor.visitTypeInsn(CHECKCAST, name);
				return;
			} catch (IllegalArgumentException e) {
			}
		}

		throw new IllegalStateException(this.member + " " + toType + " --/--> " + fromType);
	}

	public static final class Header {
		public int access;
		public String name, desc, sign;

		public Header(int access, String name, String desc, String sign) {
			this.access = access;
			this.name = name;
			this.desc = desc;
			this.sign = sign;
		}
	}
}
