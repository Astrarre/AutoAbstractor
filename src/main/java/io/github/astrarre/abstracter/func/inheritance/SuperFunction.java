package io.github.astrarre.abstracter.func.inheritance;

import java.lang.reflect.Type;

import com.google.common.reflect.TypeToken;
import io.github.astrarre.abstracter.AbstracterConfig;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings ("UnstableApiUsage")
public interface SuperFunction {
	SuperFunction EMPTY = (config, c, i) -> Object.class;

	SuperFunction BASE_DEFAULT = (config, c, i) -> {
		Class<?> current = c;
		while (config.isMinecraft(current)) {
			current = current.getSuperclass();
			if(current == null) {
				return null;
			}
		}

		return TypeToken.of(c).resolveType(current).getType();
	};

	Type findValidSuper(AbstracterConfig config, Class<?> cls, boolean impl);
}
