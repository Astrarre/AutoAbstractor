package io.github.astrarre.abstracter.abs.field;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

public class BaseFieldAbstracter extends FieldAbstracter {
	public BaseFieldAbstracter(AbstracterConfig config, AbstractAbstracter abstracter, Field member, boolean impl) {
		super(config, abstracter, member, impl);
	}

	@Override
	protected boolean isConstant(Header header) {
		int access = header.access;
		if (Modifier.isStatic(access) && !Modifier.isFinal(access)) {
			return false;
		}

		return header.desc.equals(Type.getDescriptor(this.member.getType()));
	}

	@Override
	public FieldNode createConstant(Header header, ClassNode node) {
		if (!this.impl || Modifier.isStatic(header.access)) {
			return super.createConstant(header, node);
		}
		return null;
	}
}
