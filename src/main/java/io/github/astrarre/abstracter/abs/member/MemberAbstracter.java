package io.github.astrarre.abstracter.abs.member;

import static org.objectweb.asm.Type.ARRAY;
import static org.objectweb.asm.Type.OBJECT;

import java.lang.reflect.Member;
import java.util.function.Consumer;

import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

public abstract class MemberAbstracter<T extends Member> implements Opcodes {
	protected final AbstracterConfig config;
	protected final AbstractAbstracter abstracter;
	protected final T member;
	protected final boolean impl;

	public MemberAbstracter(AbstracterConfig config, AbstractAbstracter abstracter, T member, boolean impl) {
		this.config = config;
		this.abstracter = abstracter;
		this.member = member;
		this.impl = impl;
	}

	public void cast(AbstractAbstracter.Location location,
			org.objectweb.asm.Type fromType,
			org.objectweb.asm.Type toType,
			MethodNode visitor,
			Consumer<MethodVisitor> apply) {
		if (fromType.equals(toType)) {
			apply.accept(visitor);
			return;
		}

		if (fromType.getSort() == OBJECT) {
			String internalName = fromType.getInternalName();
			AbstractAbstracter abstracter = this.config.getInterfaceAbstraction(internalName);
			if (abstracter != null) {
				if (toType.getDescriptor().equals(abstracter.getDesc(location))) {
					abstracter.castToMinecraft(visitor, apply, location);
					return;
				} else {
					throw new IllegalStateException(toType + " --/--> " + abstracter.getDesc(location));
				}
			}
		}

		if (toType.getSort() == OBJECT) {
			AbstractAbstracter abstracter = this.config.getInterfaceAbstraction(toType.getInternalName());
			if (abstracter != null) {
				if (fromType.getDescriptor().equals(abstracter.getDesc(location))) {
					abstracter.castToCurrent(visitor, apply, location);
					return;
				} else {
					throw new IllegalStateException(toType + " --/--> " + abstracter.getDesc(location));
				}
			}
		}

		if (toType.getSort() == OBJECT && fromType.getSort() == OBJECT) {
			Class<?> from = this.config.getClass(fromType.getInternalName()), to = this.config.getClass(toType.getInternalName());

			if (!to.isAssignableFrom(from)) {
				apply.accept(visitor);
				visitor.visitTypeInsn(CHECKCAST, org.objectweb.asm.Type.getInternalName(to));
			}
		}

		if(fromType.getSort() == ARRAY && toType.getSort() == ARRAY) {
			try {
				String name = fromType.getInternalName();
				this.config.getClass(name);
				visitor.visitTypeInsn(CHECKCAST, name);
				return;
			} catch (IllegalArgumentException e) {
			}
		}

		throw new IllegalStateException(this.member + " " + fromType + " --/--> " + toType + " in " + this.abstracter.name);
	}

	public static final class Header {
		public int access;
		public String name, desc, sign;

		public Header(int access, String name, String desc, String sign) {
			this.access = access;
			this.name = name;
			this.desc = desc;
			this.sign = sign;
		}
	}
}
