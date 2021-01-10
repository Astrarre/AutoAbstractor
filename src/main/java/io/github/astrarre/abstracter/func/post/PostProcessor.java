package io.github.astrarre.abstracter.func.post;

import io.github.astrarre.abstracter.AbstracterConfig;
import org.objectweb.asm.tree.ClassNode;

public interface PostProcessor {
	PostProcessor EMPTY = (config, c, n, i) -> {};

	/**
	 * Applies any last minute changes to a class
	 */
	void process(AbstracterConfig config, Class<?> cls, ClassNode node, boolean impl);

	default PostProcessor andThen(PostProcessor then) {
		return (config, c, n, i) -> {
			this.process(config, c, n, i);
			then.process(config, c, n, i);
		};
	}
}
