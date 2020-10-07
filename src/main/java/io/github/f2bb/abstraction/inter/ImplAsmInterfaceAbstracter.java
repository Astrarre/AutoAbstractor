package io.github.f2bb.abstraction.inter;

import java.lang.reflect.Modifier;

import io.github.f2bb.classpath.AbstractorClassLoader;
import io.github.f2bb.util.AsmUtil;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class ImplAsmInterfaceAbstracter extends ApiAsmInterfaceAbstracter {
	public ImplAsmInterfaceAbstracter(AbstractorClassLoader loader, Class<?> cls) {
		super(loader, cls);
	}

	@Override
	public MethodVisitor visitMethod(int access,
			String name,
			String descriptor,
			String signature,
			String[] exceptions) {
		if ((ACC_PUBLIC & access) != 0) {
			MethodVisitor visitor = super.visitMethod(access,
					name,
					this.loader.remap(descriptor),
					this.loader.remap(signature),
					null);
			int opcode;
			if(this.cls.isInterface()) opcode = INVOKEINTERFACE;
			else opcode = INVOKEVIRTUAL;
			this.invokeMethod(access, name, descriptor, visitor, opcode);
		}
		return null;
	}

	public void invokeMethod(int access, String name, String desc, MethodVisitor visitor, int instanceOpcode) {
		Type type = Type.getMethodType(desc);
		Type[] types = type.getArgumentTypes();
		int inc;
		int opcode;
		if (Modifier.isStatic(access)) {
			inc = 0;
			opcode = INVOKESTATIC;
		} else {
			inc = 1;
			visitor.visitVarInsn(ALOAD, 0);
			opcode = instanceOpcode;
		}

		for (int i = 0; i < types.length; i++) {
			visitor.visitVarInsn(types[i].getOpcode(ILOAD), i + inc);
		}

		visitor.visitMethodInsn(opcode, getInternalName(this.cls), name, type.getDescriptor(), false);
		visitor.visitInsn(type.getReturnType().getOpcode(IRETURN));
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if ((ACC_PUBLIC & access) != 0) {
			FieldVisitor get, set = null;
			if ((access & ACC_FINAL) == 0) {
				set = AsmUtil.generateSetter(super::visitMethod,
						this.name,
						access,
						name,
						this.loader.remap(descriptor),
						this.loader.remap(signature),
						true);
			}
			get = AsmUtil.generateGetter(super::visitMethod,
					this.name,
					access,
					name,
					this.loader.remap(descriptor),
					this.loader.remap(signature),
					true);
			// todo visit setter as well
			return new FieldVisitor(ASM9) {
				@Override
				public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
					return get.visitAnnotation(descriptor, visible);
				}
			};
		}
		return null;
	}
}
