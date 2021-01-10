package io.github.astrarre.abstracter.util;

import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.LongFunction;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;

import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import io.github.astrarre.abstracter.ex.InvalidClassException;
import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class AsmUtil {
	public static final Map<Class<?>, AbstractAbstracter.Location[]> TYPE_PARAMETER_REGISTRY = new HashMap<>();
	private static final String RUNTIME_EXCEPTION = getInternalName(RuntimeException.class);

	static {
		regGuess(Function.class);
		regGuess(BiFunction.class);
		regGuess(BinaryOperator.class);
		regGuess(BiPredicate.class);
		regGuess(Consumer.class);
		regGuess(DoubleFunction.class);
		regGuess(IntFunction.class);
		regGuess(LongFunction.class);
		regGuess(ObjDoubleConsumer.class);
		regGuess(ObjIntConsumer.class);
		regGuess(ObjLongConsumer.class);
		regGuess(Predicate.class);
		regGuess(Supplier.class);
		regGuess(ToDoubleBiFunction.class);
		regGuess(ToDoubleFunction.class);
		regGuess(ToIntBiFunction.class);
		regGuess(ToIntFunction.class);
		regGuess(ToLongBiFunction.class);
		regGuess(ToLongFunction.class);
		regGuess(ToLongBiFunction.class);
		regGuess(ToLongFunction.class);
		regGuess(UnaryOperator.class);
	}

	public static void regGuess(Class<?> function) {
		for (Method method : function.getMethods()) {
			if (Modifier.isAbstract(method.getModifiers())) {
				registerDefault(function, method);
				return;
			}
		}
	}


	public static void registerDefault(Class<?> function, Method method) {
		TypeVariable<?>[] variables = function.getTypeParameters();
		AbstractAbstracter.Location[] locations = new AbstractAbstracter.Location[variables.length];

		Arrays.fill(locations, AbstractAbstracter.Location.THIS);
		for (Type type : method.getGenericParameterTypes()) {
			int i = ArrayUtils.indexOf(variables, type);
			if (i != -1) {
				locations[i] = AbstractAbstracter.Location.PARAMETER;
			}
		}

		int i = ArrayUtils.indexOf(variables, method.getGenericReturnType());
		if (i != -1) {
			locations[i] = AbstractAbstracter.Location.RETURN;
		}

		TYPE_PARAMETER_REGISTRY.put(function, locations);
	}

	public static void visit(AbstracterConfig config, SignatureVisitor visitor, TypeVariable<?>[] variables) {
		for (TypeVariable<?> variable : variables) {
			visitor.visitFormalTypeParameter(variable.getName());
			boolean first = true;
			for (Type bound : variable.getBounds()) {
				if (first) {
					visitor.visitClassBound();
					first = false;
				} else {
					visitor.visitInterfaceBound();
				}
				visit(config, visitor, bound);
			}
		}
	}

	public static void visit(AbstracterConfig config, SignatureVisitor visitor, Type type) {
		visit(config, visitor, type, true);
	}

	public static void visit(AbstractAbstracter.Location location, AbstracterConfig config, SignatureVisitor visitor, Type type, boolean visitEnd) {
		if (type instanceof Class<?>) {
			Class<?> c = (Class<?>) type;
			if (c.isArray()) {
				visit(config, visitor.visitArrayType(), c.getComponentType());
			} else if (c.isPrimitive()) {
				visitor.visitBaseType(getDescriptor(c).charAt(0));
			} else {
				visitClass(location, visitor, config, c);
				if (visitEnd) {
					visitor.visitEnd();
				}
			}
			return;
		} else if (type instanceof GenericArrayType) {
			visit(config, visitor.visitArrayType(), ((GenericArrayType) type).getGenericComponentType());
			return;
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			Type owner = pt.getOwnerType();
			Class<?> raw = (Class<?>) pt.getRawType();

			boolean visitParams = true;
			if (owner != null) {
				visit(config, visitor, owner, false);
				visitor.visitInnerClassType(raw.getSimpleName());
			} else {
				// visit the type
				if (raw.isPrimitive()) {
					visitor.visitBaseType(getDescriptor(raw).charAt(0));
				} else {
					visitClass(location, visitor, config, raw);
				}
			}

			if (visitParams) {
				AbstractAbstracter.Location[] locations = TYPE_PARAMETER_REGISTRY.get(raw);
				Type[] args = pt.getActualTypeArguments();
				for (int i = 0; i < args.length; i++) {
					Type arg = args[i];
					if (!(arg instanceof WildcardType)) {
						visitor.visitTypeArgument('=');
					}
					visit(locations == null ? AbstractAbstracter.Location.THIS : locations[i], config, visitor, arg, true);
				}
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
				visit(config, visitor, l);
			}
			return;
		} else if (type == null) {
			return;
		}
		throw new IllegalArgumentException("Unrecognized type " + type + " " + type.getClass());
	}

	private static boolean visitClass(AbstractAbstracter.Location location, SignatureVisitor visitor, AbstracterConfig config, Class<?> raw) {
		String internal = getInternalName(raw);
		AbstractAbstracter abstracter = config.getInterfaceAbstraction(internal);
		if (abstracter != null) {
			return abstracter.visitSign(location, visitor);
		} else {
			Class<?> cls = config.getClass(internal);
			if (config.isMinecraft(cls)) {
				throw new InvalidClassException(cls);
			}

			visitor.visitClassType(internal);
		}
		return true;
	}

	public static void visit(AbstracterConfig config, SignatureVisitor visitor, Type type, boolean visitEnd) {
		visit(AbstractAbstracter.Location.THIS, config, visitor, type, visitEnd);
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

	// todo these need special casing for Consumer stuff, tbh I could just add a global function for this kind of stuff
	public static String toSignature(AbstracterConfig config, Type reified) {
		return toSignature(AbstractAbstracter.Location.THIS, config, reified);
	}

	public static String toSignature(AbstractAbstracter.Location location, AbstracterConfig config, Type reified) {
		SignatureWriter writer = new SignatureWriter();
		visit(location, config, writer, reified, true);
		return writer.toString();
	}

	public static <A, B> B[] map(A[] arr, Function<A, B> func, IntFunction<B[]> array) {
		B[] bs = array.apply(arr.length);
		for (int i = 0; i < arr.length; i++) {
			bs[i] = func.apply(arr[i]);
		}
		return bs;
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
			visitor.visitTypeInsn(Opcodes.NEW, RUNTIME_EXCEPTION);
			visitor.visitInsn(Opcodes.DUP);
			visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, RUNTIME_EXCEPTION, "<init>", "()V", false);
			visitor.visitInsn(Opcodes.ATHROW);
		}
	}

	public static MethodNode findMethod(ClassNode node, String name, String desc) {
		for (MethodNode method : node.methods) {
			if (name.equals(method.name) && desc.equals(method.desc)) {
				return method;
			}
		}
		throw new IllegalArgumentException("unable to find " + name + desc + " in " + node.name);
	}

	public static String getInterfaceDesc(AbstracterConfig config, Class<?> cls) {
		if (cls.isPrimitive()) {
			return getDescriptor(cls);
		} else if (cls.isArray()) {
			return '[' + getInterfaceDesc(config, cls.getComponentType());
		} else {
			return "L" + config.getInterfaceName(cls) + ";";
		}
	}
}
