package io.github.f2bb.stripper.impl;

import io.github.f2bb.stripper.StripAsm;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ReplacerAsm extends ClassVisitor {
	public ReplacerAsm(ClassVisitor visitor) {
		super(Opcodes.ASM9, visitor);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if (StripAsm.keep(access)) {
			return super.visitField(access, name, descriptor, signature, value);
		}
		return null;
	}

	@Override
	public MethodVisitor visitMethod(int access,
			String name,
			String descriptor,
			String signature,
			String[] exceptions) {
		if (StripAsm.keep(access)) {
			return super.visitMethod(access, name, descriptor, signature, exceptions);
		}
		return null;
	}
}
