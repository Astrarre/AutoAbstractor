package io.github.f2bb.abstracter.func.postprocess;

import com.squareup.javapoet.TypeSpec;
import org.objectweb.asm.tree.ClassNode;

public interface PostProcessor {
	PostProcessor NOTHING = new PostProcessor() {
		// @formatter:off
		@Override public void processAsm(ClassNode header, Class<?> cls, boolean impl) {}
		@Override public void processJava(TypeSpec.Builder header, Class<?> cls, boolean impl) {}
		// @formatter:on
	};

	default PostProcessor andThen(PostProcessor processor) {
		return new PostProcessor() {
			@Override
			public void processAsm(ClassNode header, Class<?> cls, boolean impl) {
				PostProcessor.this.processAsm(header, cls, impl);
				processor.processAsm(header, cls, impl);
			}

			@Override
			public void processJava(TypeSpec.Builder header, Class<?> cls, boolean impl) {
				PostProcessor.this.processJava(header, cls, impl);
				processor.processJava(header, cls, impl);
			}
		};
	}

	void processAsm(ClassNode header, Class<?> cls, boolean impl);

	void processJava(TypeSpec.Builder header, Class<?> cls, boolean impl);
}
