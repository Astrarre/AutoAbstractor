package io.github.f2bb.abstracter.func.map;

import java.lang.reflect.Type;
import java.util.function.Function;

import com.google.common.reflect.TypeToken;

@SuppressWarnings ("UnstableApiUsage")
public interface TypeMappingFunction {
	Type map(Type in);

	static TypeMappingFunction reify(Class<?> view) {
		TypeToken<?> token = TypeToken.of(view);
		return t -> token.resolveType(t).getType();
	}

	static Type reify(Class<?> view, Type type) {
		TypeToken<?> token = TypeToken.of(view);
		return token.resolveType(type).getType();
	}

	static Function<Type, Class<?>> raw(Class<?> view) {
		TypeToken<?> token = TypeToken.of(view);
		return t -> token.resolveType(t).getRawType();
	}

	static Class<?> raw(Class<?> view, Type type) {
		TypeToken<?> token = TypeToken.of(view);
		return token.resolveType(type).getRawType();
	}

	static Function<Type, TypeToken<?>> resolve(Class<?> view) {
		TypeToken<?> token = TypeToken.of(view);
		return token::resolveType;
	}

	static TypeToken<?> resolve(Class<?> view, Type type) {
		return resolve(view).apply(type);
	}
}
