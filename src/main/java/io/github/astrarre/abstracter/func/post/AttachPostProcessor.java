package io.github.astrarre.abstracter.func.post;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.util.AsmUtil;
import org.objectweb.asm.tree.ClassNode;

public class AttachPostProcessor implements PostProcessor {
	public static final String OBJECT = org.objectweb.asm.Type.getInternalName(Object.class);

	private final Type type;
	public AttachPostProcessor(Type type) {this.type = type;}

	@Override
	public void process(AbstracterConfig config, Class<?> cls, ClassNode node, boolean impl) {
		// append to signature
		if(this.type instanceof ParameterizedType) {
			// if no signature to append to
			if (node.signature == null) {
				node.signature = "L" + (node.superName == null ? OBJECT : node.superName) + ";";
				for (String iface : node.interfaces) {
					node.signature += 'L' + iface + ';';
				}
			}

			node.signature += AsmUtil.toSignature(config, this.type);
		}

		node.interfaces.add(org.objectweb.asm.Type.getInternalName(AsmUtil.raw(this.type)));
	}
}
