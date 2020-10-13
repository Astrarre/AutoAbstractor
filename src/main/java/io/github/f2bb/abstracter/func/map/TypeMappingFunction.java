package io.github.f2bb.abstracter.func.map;

import java.lang.reflect.Type;

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
}
