package io.github.f2bb.abstracter.func.inheritance;

import java.lang.reflect.Type;

import com.google.common.reflect.TypeToken;
import io.github.f2bb.abstracter.Abstracter;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings ("UnstableApiUsage")
public interface SuperFunction {
	SuperFunction EMPTY = c -> null;

	SuperFunction BASE_API_DEFAULT = c -> {
		Class<?> current = c;
		while (Abstracter.isMinecraft(current)) {
			current = current.getSuperclass();
		}
		return TypeToken.of(c).resolveType(current).getType();
	};

	SuperFunction INTERFACE_DEFAULT = c -> null;

	SuperFunction BASE_IMPL_DEFAULT = c -> c;

	@Nullable
	Type findValidSuper(Class<?> cls);

}
