package io.github.f2bb.abstracter.func.inheritance;

import java.lang.reflect.Type;

import com.google.common.reflect.TypeToken;
import io.github.f2bb.abstracter.util.AbstracterLoader;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings ("UnstableApiUsage")
public interface SuperFunction {
	SuperFunction EMPTY = (c, i) -> Object.class;

	SuperFunction BASE_DEFAULT = (c, i) -> {
		if(i) {
			return c;
		}

		Class<?> current = c;
		while (AbstracterLoader.isMinecraft(current)) {
			current = current.getSuperclass();
		}
		return TypeToken.of(c).resolveType(current).getType();
	};

	@Nullable
	Type findValidSuper(Class<?> cls, boolean impl);

}
