package io.github.f2bb.abstraction.base;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

import io.github.f2bb.api.ImplementationHiddenException;
import io.github.f2bb.asm.AnnotationReader;
import io.github.f2bb.classpath.AbstractorClassLoader;
import io.github.f2bb.util.AsmUtil;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

// todo filter out public methods from being exposed, cus the interface already exposes them
public class ApiAsmBaseAbstracter extends ImplAsmBaseAbstracter {
	public ApiAsmBaseAbstracter(AbstractorClassLoader loader, Class<?> cls) {
		super(loader, cls);
	}

	@Override
	public void visit(Field field, java.lang.reflect.Type type) {
		int access = field.getModifiers();
		Class<?> raw = this.raw(field.getType());
		String desc = Type.getDescriptor(raw);
		String sign = this.toSignature(type, false);
		FieldVisitor getter, setter = null;

		if (raw != field.getType() || Modifier.isStatic(access) || this.loader.isMinecraftDesc(desc)) {
			getter = AsmUtil.generateGetter(super::visitMethod, this.name, access, field.getName(), desc, sign, false);
			if (!Modifier.isFinal(access)) {
				setter = AsmUtil.generateSetter(super::visitMethod, this.name, access, field.getName(), desc, sign, false);
			}
		} else {
			// did you know? fields have virtual lookups!
			getter = super.visitField(access, this.name, desc, sign, null);
			setter = null;
		}

		for (Annotation annotation : field.getAnnotations()) {
			AnnotationReader.visit(annotation, getter);
			if (setter != null) {
				AnnotationReader.visit(annotation, setter);
			}
		}
	}

	@Override
	public void visitBridge(Method method, String target) {}

	@Override
	public void postVisit(MethodVisitor visitor, Executable method) {
		for (Annotation annotation : method.getAnnotations()) {
			AnnotationReader.visit(annotation, visitor);
		}

		for (Parameter parameter : method.getParameters()) {
			visitor.visitParameter(parameter.getName(), parameter.getModifiers());
		}
	}

	@Override
	public void invokeMethod(int access, String name, String desc, MethodVisitor visitor, int instanceOpcode) {
		if (name.equals("<init>")) {
			// technically to be perfect we'd call the super class constructor with bogus parameters, but that's too much effort
			super.invokeMethod(access, name, desc, visitor, instanceOpcode);
		} else {
			AsmUtil.visitStub(visitor);
		}
	}
}
