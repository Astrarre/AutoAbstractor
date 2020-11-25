package io.github.astrarre.abstracter.func.post;

import org.objectweb.asm.tree.ClassNode;

public interface PostProcessor {
	PostProcessor EMPTY = (c, n, i) -> {};

	/**
	 * Applies any last minute changes to a class
	 */
	void process(Class<?> cls, ClassNode node, boolean impl);

	default PostProcessor andThen(PostProcessor then) {
		return (c, n, i) -> {
			this.process(c, n, i);
			then.process(c, n, i);
		};
	}
}
