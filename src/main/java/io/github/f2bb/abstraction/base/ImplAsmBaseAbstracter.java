package io.github.f2bb.abstraction.base;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import io.github.f2bb.abstraction.AbstractAbstracter;
import io.github.f2bb.abstraction.AbstractBaseAbstracter;
import io.github.f2bb.classpath.AbstractorClassLoader;
import io.github.f2bb.reflect.ReifiedType;
import io.github.f2bb.util.AsmUtil;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

// todo filter out all declaring class != minecraft
// todo you're missing all the casts
public class ImplAsmBaseAbstracter extends AbstractBaseAbstracter {
	public ImplAsmBaseAbstracter(AbstractorClassLoader loader, Class<?> cls) {
		super(new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES), loader, cls);
	}

	@Override
	public void visit(int version, int access, String name, TypeVariable<?>[] typeVariables, ReifiedType superClass, Collection<ReifiedType> interfaces) {
		// generate class signature
		String signature = this.toSignature(this.cls.getTypeParameters(),
		                                    this.prefixSign("Base", superClass.raw, superClass.type),
		                                    interfaces.stream().map(ReifiedType::getType).collect(Collectors.toList())) + this.prefixSign("I", this.cls, this.cls); // todo this needs to output the type variables
		super.visit(version, access, AsmUtil.prefixName("Base", name), // name
		            signature, // signature
		            this.prefix("Base", superClass.raw), // super
		            // todo add this class to interfaces
		            interfaces.stream().map(ReifiedType::getRaw).map(AbstractAbstracter::getInternalName).toArray(String[]::new));

	}

	@Override
	public void visit(Constructor constructor, TypeVariable<?>[] variable, java.lang.reflect.Type[] parameters) {
		int access = constructor.getModifiers();
		String name = "<init>";
		String sign = this.createMethodSignature(null, parameters, void.class);
		String desc = this.createMethodSignature(variable, this.raw(parameters), void.class);
		MethodVisitor visitor = super.visitMethod(access, name, desc, sign, null);
		this.invokeMethod(0, name, Type.getConstructorDescriptor(constructor), visitor, INVOKESPECIAL);
		this.postVisit(visitor, constructor);
	}

	@Override
	public void visit(Method method, TypeVariable<?>[] variable, java.lang.reflect.Type[] parameters, java.lang.reflect.Type returnType) {
		int access = method.getModifiers();
		String desc = this.visitBridged(method, access, parameters, returnType);
		this.visitBridge(method, desc);
	}

	@Override
	public void visit(Field field, java.lang.reflect.Type type) {
		int access = field.getModifiers();
		// public members are handled by interface abstraction
		String desc = Type.getDescriptor(field.getType());
		if (Modifier.isStatic(access) || this.loader.isMinecraftDesc(desc)) {
			AsmUtil.generateGetter(super::visitMethod, this.name, access, field.getName(), desc, null, true);
			if (!Modifier.isFinal(access)) {
				AsmUtil.generateSetter(super::visitMethod, this.name, access, field.getName(), desc, null, true);
			}
		}
	}


	/**
	 * write annotations and parameters
	 */
	public void postVisit(MethodVisitor visitor, Executable executable) {}

	public void visitBridge(Method method, String target) {
		Type type = Type.getType(method);
		MethodVisitor visitor = super.visitMethod(ACC_PUBLIC | ACC_FINAL, method.getName(), type.getDescriptor(), null, null);
		int opcode;
		if (method.getDeclaringClass().isInterface()) {
			opcode = INVOKEINTERFACE;
		} else {
			opcode = INVOKEVIRTUAL;
		}
		this.invokeMethod(method.getModifiers(), method.getName(), target, visitor, opcode);
	}

	public String visitBridged(Method method, int access, java.lang.reflect.Type[] parameters, java.lang.reflect.Type returnType) {
		String sign = this.createMethodSignature(null, parameters, returnType);
		String desc = this.createMethodSignature(null, this.raw(parameters), this.raw(returnType));
		MethodVisitor visitor = super.visitMethod(access, method.getName(), desc, sign, null);
		if (!Modifier.isAbstract(access)) {
			this.invokeMethod(access, method.getName(), Type.getMethodDescriptor(method), visitor, INVOKESPECIAL);
		}
		this.postVisit(visitor, method);
		return desc;
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
	public Optional<Resource> write() {
		return Optional.of(new Resource() {
			@Override
			public String getPath() {
				return AsmUtil.prefixName("Base", ImplAsmBaseAbstracter.this.name) + ".class";
			}

			@Override
			public void write(OutputStream stream) throws IOException {
				stream.write(((ClassWriter) ImplAsmBaseAbstracter.this.cv).toByteArray());
			}
		});
	}
}
