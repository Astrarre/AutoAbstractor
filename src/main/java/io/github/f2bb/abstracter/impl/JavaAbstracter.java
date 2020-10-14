package io.github.f2bb.abstracter.impl;

import static io.github.f2bb.old.util.AbstracterUtil.map;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import io.github.f2bb.abstracter.Abstracter;

public class JavaAbstracter {
	public static final Constructor<WildcardTypeName> WILDCARD_TYPE_NAME_CONSTRUCTOR;
	public static final Method METHOD_GENERIC;
	public static final Method FIELD_GENERIC;
	public static final Field CONSTRUCTOR_GENERIC;
	static {
		try {
			WILDCARD_TYPE_NAME_CONSTRUCTOR = WildcardTypeName.class.getDeclaredConstructor(List.class,
					List.class,
					List.class);
			WILDCARD_TYPE_NAME_CONSTRUCTOR.setAccessible(true);
			METHOD_GENERIC = Method.class.getDeclaredMethod("getGenericSignature");
			METHOD_GENERIC.setAccessible(true);
			FIELD_GENERIC = Field.class.getDeclaredMethod("getGenericSignature");
			FIELD_GENERIC.setAccessible(true);
			CONSTRUCTOR_GENERIC = Constructor.class.getDeclaredField("signature");
			CONSTRUCTOR_GENERIC.setAccessible(true);
		} catch (NoSuchMethodException | NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}

	// automatically remapped
	public static TypeName toTypeName(Type type) {
		if (type instanceof Class<?>) {
			Class<?> cls = (Class<?>) type;
			if (Abstracter.isMinecraft(cls)) {
				String name = Abstracter.getInterfaceName(cls);
				int pkgIndex = Math.max(name.lastIndexOf('/'), 0);
				String pkg = name.substring(0, pkgIndex);
				int innerIndex = name.indexOf('$', pkgIndex);
				if (innerIndex > 0) {
					return ClassName.get(pkg.replace('/', '.'),
							name.substring(pkgIndex + 1, innerIndex),
							name.substring(innerIndex + 1).split("\\$"));
				} else {
					return ClassName.get(pkg.replace('/', '.'), name.substring(pkgIndex + 1));
				}
			}
			if (!cls.isPrimitive()) {
				return ClassName.get(cls);
			} else {
				return TypeName.get(cls);
			}
		} else if (type instanceof GenericArrayType) {
			return ArrayTypeName.of(toTypeName(((GenericArrayType) type).getGenericComponentType()));
		} else if (type instanceof ParameterizedType) {
			ParameterizedType ptn = (ParameterizedType) type;
			return ParameterizedTypeName.get((ClassName) toTypeName(ptn.getRawType()),
					map(ptn.getActualTypeArguments(), JavaAbstracter::toTypeName, TypeName[]::new));
		} else if (type instanceof TypeVariable<?>) {
			TypeVariable<?> tvn = (TypeVariable<?>) type;
			return TypeVariableName.get(tvn.getName(), map(tvn.getBounds(), JavaAbstracter::toTypeName, TypeName[]::new));
		} else if (type instanceof WildcardType) {
			WildcardType wtn = (WildcardType) type;
			return get(map(wtn.getLowerBounds(), JavaAbstracter::toTypeName), map(wtn.getUpperBounds(), JavaAbstracter::toTypeName));
		}
		throw new IllegalArgumentException("What " + type);
	}

	/**
	 * default not included
	 */
	public static Set<Modifier> getModifiers(int modifier) {
		Set<Modifier> modifiers = new HashSet<>();
		if (java.lang.reflect.Modifier.isAbstract(modifier)) {
			modifiers.add(Modifier.ABSTRACT);
		}
		if (java.lang.reflect.Modifier.isTransient(modifier)) {
			modifiers.add(Modifier.TRANSIENT);
		}
		if (java.lang.reflect.Modifier.isFinal(modifier)) {
			modifiers.add(Modifier.FINAL);
		}
		if (java.lang.reflect.Modifier.isNative(modifier)) {
			modifiers.add(Modifier.NATIVE);
		}
		if (java.lang.reflect.Modifier.isPrivate(modifier)) {
			modifiers.add(Modifier.PRIVATE);
		}
		if (java.lang.reflect.Modifier.isProtected(modifier)) {
			modifiers.add(Modifier.PROTECTED);
		}
		if (java.lang.reflect.Modifier.isPublic(modifier)) {
			modifiers.add(Modifier.PUBLIC);
		}
		if (java.lang.reflect.Modifier.isStatic(modifier)) {
			modifiers.add(Modifier.STATIC);
		}
		if (java.lang.reflect.Modifier.isStrict(modifier)) {
			modifiers.add(Modifier.STRICTFP);
		}
		if (java.lang.reflect.Modifier.isSynchronized(modifier)) {
			modifiers.add(Modifier.SYNCHRONIZED);
		}
		if (java.lang.reflect.Modifier.isVolatile(modifier)) {
			modifiers.add(Modifier.VOLATILE);
		}
		return modifiers;
	}

	public static WildcardTypeName get(List<TypeName> upper, List<TypeName> lower) {
		try {
			return WILDCARD_TYPE_NAME_CONSTRUCTOR.newInstance(upper, lower, Collections.emptyList());
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new IllegalStateException(e);
		}
	}
}
