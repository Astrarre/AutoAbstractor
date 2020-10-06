package io.github.f2bb.abstraction;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.google.common.reflect.TypeToken;
import io.github.f2bb.classpath.AbstractorClassLoader;
import io.github.f2bb.util.AsmUtil;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

// todo in java abstracter, ignore requests to abstract inner classes
public abstract class AbstractAbstracter extends ClassVisitor implements Opcodes {
	protected final AbstractorClassLoader loader;
	protected final Class<?> cls;
	protected final TypeToken<?> token;

	protected AbstractAbstracter(AbstractorClassLoader loader, Class<?> cls) {
		this(null, loader, cls);
	}

	public AbstractAbstracter(ClassVisitor classVisitor, AbstractorClassLoader loader, Class<?> cls) {
		super(ASM9, classVisitor);
		this.loader = loader;
		this.cls = cls;
		this.token = TypeToken.of(cls);
	}

	public String toSignature(Type[] typeParameters, Type superClass, Collection<Type> interfaces) {
		StringBuilder builder = new StringBuilder();
		if (typeParameters.length > 0) {
			builder.append('<');
			for (Type var : typeParameters) {
				builder.append(this.toSignature(var, true));
			}
			builder.append('>');
		}

		if (superClass != null) {
			builder.append(this.toSignature(superClass, false));
		}
		for (Type anInterface : interfaces) {
			builder.append(this.toSignature(anInterface, false));
		}
		return builder.toString();
	}

	public String createMethodSignature(@Nullable TypeVariable<Method>[] variables, Type[] parameters, Type ret) {
		StringBuilder builder = new StringBuilder();
		if (variables != null && variables.length > 0) {
			builder.append('<');
			for (TypeVariable<Method> variable : variables) {
				builder.append(this.toSignature(this.token.resolveType(variable).getType(), true));
			}
			builder.append('>');
		}
		builder.append('(');
		for (Type parameter : parameters) {
			builder.append(this.toSignature(this.token.resolveType(parameter).getType(), false));
		}
		builder.append(')');
		System.out.println("\t" + ret);
		builder.append(this.toSignature(this.token.resolveType(ret).getType(), false));
		return builder.toString();
	}

	public String prefixSign(String prefix, Class<?> raw, java.lang.reflect.Type type) {
		if (this.loader.isMinecraft(raw)) {
			String sign = this.toSignature(type, false);
			int yes = sign.indexOf('<');
			if (yes == -1) {
				yes = Integer.MAX_VALUE;
			}
			int i = sign.lastIndexOf('/', yes) + 1;
			return sign.substring(0, i) + prefix + sign.substring(i);
		}
		return this.toSignature(type, false);
	}

	public String prefix(String prefix, Class<?> name) {
		if (this.loader.isMinecraft(name)) {
			return AsmUtil.prefixName(prefix, org.objectweb.asm.Type.getInternalName(name));
		}
		return org.objectweb.asm.Type.getInternalName(name);
	}

	public String prefix(String prefix, String name) {
		if (this.loader.isMinecraft(name.replace('/', '.'))) {
			return AsmUtil.prefixName(prefix, name);
		}
		return name;
	}

	protected Type reify(Type type) {
		return this.token.resolveType(type).getType();
	}
	protected Type raw(Type type) {
		return this.token.resolveType(type).getRawType();
	}

	protected Class<?> getValidSuper() {
		Class<?> cls = this.cls;
		do {
			cls = cls.getSuperclass();
		} while (!this.loader.isValidClass(cls));
		return cls;
	}

	public Type[] raw(Type[] arr) {
		for (int i = 0; i < arr.length; i++) {
			arr[i] = this.raw(arr[i]);
		}
		return arr;
	}

	public Type[] reify(Type[] arr) {
		for (int i = 0; i < arr.length; i++) {
			arr[i] = this.reify(arr[i]);
		}
		return arr;
	}
	public Set<Type> reify(Set<Class<?>> faces) {
		Set<Type> reified = new HashSet<>();
		for (Class<?> face : faces) {
			reified.add(this.reify(face));
		}
		return reified;
	}

	public Set<Class<?>> getInterfaces(Class<?> cls) {
		Set<Class<?>> faces = new HashSet<>();
		do {
			for (Class<?> i : cls.getInterfaces()) {
				if (this.loader.isValidClass(i)) {
					faces.add(i);
				} else {
					faces.addAll(this.getInterfaces(i));
				}
			}

			cls = cls.getSuperclass();
			if (cls == null) {
				break;
			}
		} while (!this.loader.isValidClass(cls));
		return faces;
	}

	public abstract Optional<Resource> write();

	public abstract static class Resource {
		abstract String getPath();

		abstract void write(OutputStream stream) throws IOException;
	}

	public String toSignature(Type type, boolean shouldBound) {
		if (type instanceof TypeVariable) {
			TypeVariable<?> variable = (TypeVariable<?>) type;
			StringBuilder builder = new StringBuilder();
			if (shouldBound) {
				builder.append(variable.getName());
				Type[] bounds = variable.getBounds();
				for (Type bound : bounds) {
					builder.append(':');
					if (bound != Object.class || bounds.length == 1) {
						builder.append(this.toSignature(bound, false));
					}
				}
			} else {
				builder.append('T').append(variable.getName()).append(';');
			}
			return builder.toString();
		} else if (type instanceof GenericArrayType) {
			return "[" + this.toSignature(((GenericArrayType) type).getGenericComponentType(), false);
		} else if (type instanceof ParameterizedType) {
			ParameterizedType par = (ParameterizedType) type;
			StringBuilder builder = new StringBuilder();
			builder.append(this.toSignature(par.getRawType(), shouldBound));
			// cut off semicolon
			int last = builder.length() - 1;
			if (builder.charAt(last) == ';') {
				builder.setLength(last);
			}
			Type[] actual = par.getActualTypeArguments();
			if (actual.length != 0) {
				builder.append('<');
				for (Type t : actual) {
					builder.append(this.toSignature(t, false));
				}
				builder.append('>');
			}
			builder.append(';');
			return builder.toString();
		} else if (type instanceof Class) {
			return this.prefix("I", (Class<?>) type);
		} else if (type instanceof WildcardType) {
			StringBuilder builder = new StringBuilder();
			WildcardType wt = (WildcardType) type;
			Type[] upper = wt.getUpperBounds();
			if (upper.length > 0) {
				if (upper.length == 1 && upper[0] == Object.class) {
					builder.append('*');
				} else {
					builder.append('+');
					for (Type t : upper) {
						builder.append(this.toSignature(t, false));
					}
				}
			} else {
				Type[] lower = wt.getLowerBounds();
				if (lower.length > 0) {
					builder.append('-');
					for (Type t : lower) {
						builder.append(this.toSignature(t, false));
					}
				}
			}
			return builder.toString();
		}
		throw new UnsupportedOperationException("Unkown type " + type + " " + type.getClass());
	}

	public String toSignature(Type[] typeParameters, String superClass, Collection<Type> interfaces) {
		StringBuilder builder = new StringBuilder();
		if (typeParameters.length > 0) {
			builder.append('<');
			for (Type var : typeParameters) {
				builder.append(this.toSignature(var, true));
			}
			builder.append('>');
		}
		builder.append('L').append(superClass).append(';');
		for (Type anInterface : interfaces) {
			builder.append(this.toSignature(anInterface, false));
		}
		return builder.toString();
	}
}
