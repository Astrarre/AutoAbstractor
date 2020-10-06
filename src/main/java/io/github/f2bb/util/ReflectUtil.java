package io.github.f2bb.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import com.google.common.collect.Collections2;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import io.github.f2bb.classpath.AbstractorClassLoader;
import org.objectweb.asm.Opcodes;

/**
 * Common utilty methods that are useful when working with reflection.
 */
public class ReflectUtil implements Opcodes {
	/** Static helper class, shouldn't be constructed. */
	private ReflectUtil() {}

	private static final Method CLASS_SIGNATURE = Util.find(Class.class, "getGenericSignature0");
	private static final Method FIELD_SIGNATURE = Util.find(Field.class, "getGenericSignature");
	private static final Method METHOD_SIGNATURE = Util.find(Method.class, "getGenericSignature");
	private static final Method CONSTRUCTOR_SIGNATURE = Util.find(Constructor.class, "getSignature");

	public static ClassName getName(String internalName, String prefix) {
		int index = internalName.lastIndexOf('/');
		String pkg = internalName.substring(0, index).replace('/', '.');
		String[] simple = internalName.substring(index).split("\\$");
		return ClassName.get(pkg, prefix + simple[simple.length-1], Arrays.copyOfRange(simple, 0, simple.length-1));
	}

	public static TypeName map(AbstractorClassLoader classLoader, Type type) {
		return map(classLoader, TypeName.get(type));
	}

	public static TypeName map(AbstractorClassLoader classLoader, TypeName name) {
		if(name instanceof ClassName) {
			if(!name.isPrimitive()) {
				ClassName cn = (ClassName) name;
				String refName = cn.reflectionName();
				if(classLoader.isMinecraft(refName)) {
					List<String> simple = cn.simpleNames();
					return ClassName.get(cn.packageName(), "I" + simple.get(simple.size()-1), simple.subList(0, simple.size()-1).toArray(new String[0]));
				}
			}
			return name;
		} else if(name instanceof ParameterizedTypeName) {
			ParameterizedTypeName ptn = (ParameterizedTypeName) name;
			TypeName[] names = new TypeName[ptn.typeArguments.size()];
			for (int i = 0; i < ptn.typeArguments.size(); i++) {
				names[i] = map(classLoader, ptn.typeArguments.get(i));
			}
			return ParameterizedTypeName.get(ptn.rawType, names).annotated(name.annotations);
		} else if(name instanceof WildcardTypeName) {
			WildcardTypeName wtn = (WildcardTypeName) name;
			return get(wtn.upperBounds.stream().map(i -> map(classLoader, i)).collect(Collectors.toList()), wtn.upperBounds.stream().map(i -> map(classLoader, i)).collect(Collectors.toList()), wtn.annotations);
		} else if(name instanceof TypeVariableName) {
			TypeVariableName tvn = (TypeVariableName) name;
			return TypeVariableName.get(tvn.name, tvn.bounds.stream().map(i -> map(classLoader, i)).toArray(TypeName[]::new)).annotated(name.annotations);
		} else if(name instanceof ArrayTypeName) {
			return ArrayTypeName.of(map(classLoader, ((ArrayTypeName) name).componentType)).annotated(name.annotations);
		}
		throw new IllegalArgumentException("What");
	}

	private static final Constructor<WildcardTypeName> WILDCARD_TYPE_NAME_CONSTRUCTOR;

	static {
		try {
			WILDCARD_TYPE_NAME_CONSTRUCTOR = WildcardTypeName.class.getDeclaredConstructor(List.class, List.class, List.class);
			CONSTRUCTOR_SIGNATURE.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public static WildcardTypeName get(List<TypeName> upper, List<TypeName> lower, List<AnnotationSpec> annotations) {
		try {
			return WILDCARD_TYPE_NAME_CONSTRUCTOR.newInstance(upper, lower, annotations);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * default not included
	 */
	public static List<javax.lang.model.element.Modifier> getModifiers(int modifier) {
		List<javax.lang.model.element.Modifier> modifiers = new ArrayList<>();
		if(Modifier.isAbstract(modifier)) modifiers.add(javax.lang.model.element.Modifier.ABSTRACT);
		if(Modifier.isTransient(modifier)) modifiers.add(javax.lang.model.element.Modifier.TRANSIENT);
		if(Modifier.isFinal(modifier)) modifiers.add(javax.lang.model.element.Modifier.FINAL);
		if(Modifier.isNative(modifier)) modifiers.add(javax.lang.model.element.Modifier.NATIVE);
		if(Modifier.isPrivate(modifier)) modifiers.add(javax.lang.model.element.Modifier.PRIVATE);
		if(Modifier.isProtected(modifier)) modifiers.add(javax.lang.model.element.Modifier.PROTECTED);
		if(Modifier.isPublic(modifier)) modifiers.add(javax.lang.model.element.Modifier.PUBLIC);
		if(Modifier.isStatic(modifier)) modifiers.add(javax.lang.model.element.Modifier.STATIC);
		if(Modifier.isStrict(modifier)) modifiers.add(javax.lang.model.element.Modifier.STRICTFP);
		if(Modifier.isSynchronized(modifier)) modifiers.add(javax.lang.model.element.Modifier.SYNCHRONIZED);
		if(Modifier.isVolatile(modifier)) modifiers.add(javax.lang.model.element.Modifier.VOLATILE);
		return modifiers;
	}

	public static boolean isInstanceInnerClass(Class<?> cls) {
		return cls.isMemberClass() && !Modifier.isStatic(cls.getModifiers());
	}

	public static String getSignature(Class<?> cls) {
		try {
			return (String) CLASS_SIGNATURE.invoke(cls);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String getSignature(Constructor<?> constructor) {
		try {
			return (String) CONSTRUCTOR_SIGNATURE.invoke(constructor);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String getSignature(Method cls) {
		try {
			return (String) METHOD_SIGNATURE.invoke(cls);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String getSignature(Field cls) {
		try {
			return (String) FIELD_SIGNATURE.invoke(cls);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Fetches all methods of all access types from the supplied class and super classes. Methods that have been overridden in the inheritance hierarchy are only returned once, using the instance
	 * lowest down the hierarchy.
	 *
	 * @param clazz the class to inspect
	 * @return a collection of methods
	 */
	public static Collection<Method> getMethods(Class<?> clazz) {
		Collection<Method> found = new ArrayList<Method>();
		while (clazz != null) {
			for (Method m1 : clazz.getDeclaredMethods()) {
				boolean overridden = false;
				for (Method m2 : found) {
					if (m2.getName().equals(m1.getName()) && Arrays.deepEquals(m1.getParameterTypes(), m2.getParameterTypes()) && m1.getReturnType().equals(m2.getReturnType())) {
						overridden = true;
						break;
					}
				}

                if (!overridden) {
                    found.add(m1);
                }
			}

			clazz = clazz.getSuperclass();
		}

		return Collections2.filter(found, m -> m != null && !(m.isSynthetic() || m.isBridge()));
	}

	/**
	 * Fetches all fields of all access types from the supplied class and super classes. Fieldss that have been overridden in the inheritance hierarchy are only returned once, using the instance
	 * lowest down the hierarchy.
	 *
	 * @param clazz the class to inspect
	 * @return a collection of fields
	 */
	public static Collection<Field> getFields(Class<?> clazz) {
		Map<String, Field> fields = new HashMap<String, Field>();
		while (clazz != null) {
			for (Field field : clazz.getDeclaredFields()) {
				if (!fields.containsKey(field.getName())) {
					fields.put(field.getName(), field);
				}
			}

			clazz = clazz.getSuperclass();
		}

		return fields.values();
	}
}