package io.github.astrarre.abstracter.abs.field;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import org.objectweb.asm.tree.MethodNode;

public class InterfaceFieldAbstracter extends FieldAbstracter {
	public InterfaceFieldAbstracter(AbstracterConfig config, AbstractAbstracter abstracter, Field member, boolean impl) {
		super(config, abstracter, member, impl);
	}

	@Override
	public MethodNode createGetter(Header header) {
		return this.strip(super.createGetter(header));
	}

	@Override
	public MethodNode createSetter(Header header) {
		return this.strip(super.createSetter(header));
	}

	private MethodNode strip(MethodNode node) {
		node.access &= ~ACC_FINAL;
		if(!(this.impl || Modifier.isStatic(node.access))) {
			node.access |= ACC_ABSTRACT;
			node.instructions.clear();
		}
		return node;
	}
}
