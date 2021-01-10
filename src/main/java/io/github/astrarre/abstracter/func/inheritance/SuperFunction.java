package io.github.astrarre.abstracter.func.inheritance;

import java.lang.reflect.Type;

import com.google.common.reflect.TypeToken;
import io.github.astrarre.abstracter.AbstracterConfig;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings ("UnstableApiUsage")
public interface SuperFunction {
	SuperFunction EMPTY = (config, c, i) -> Object.class;

	SuperFunction BASE_DEFAULT = (config, c, i) -> {
		if(i) {
			return c;
		}

		Class<?> current = c;
		while (config.isMinecraft(current)) {
			current = current.getSuperclass();
		}
		return TypeToken.of(c).resolveType(current).getType();
	};

	@Nullable
	Type findValidSuper(AbstracterConfig config, Class<?> cls, boolean impl);

}
