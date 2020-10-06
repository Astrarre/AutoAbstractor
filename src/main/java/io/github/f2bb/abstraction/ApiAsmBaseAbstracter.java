package io.github.f2bb.abstraction;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.Set;

import io.github.f2bb.api.ImplementationHiddenException;
import io.github.f2bb.classpath.AbstractorClassLoader;
import io.github.f2bb.util.AsmUtil;
import io.github.f2bb.util.ReflectUtil;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class ApiAsmBaseAbstracter extends AbstractAbstracter {
	protected final String path;
	protected String name;

	public ApiAsmBaseAbstracter(AbstractorClassLoader loader, Class<?> cls) {
		super(new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES), loader, cls);
		this.path = Type.getInternalName(cls) + ".class";
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.name = name;
		Class<?> sup = this.getValidSuper();
		java.lang.reflect.Type reifiedSup = this.reify(sup);
		Set<Class<?>> ifaces = this.getInterfaces(this.cls);
		Set<java.lang.reflect.Type> reifiedInterfaces = this.reify(ifaces);

		signature = this.toSignature(sup.getTypeParameters(), this.prefixSign("Base", sup, reifiedSup), reifiedInterfaces) + this.prefixSign("I", this.cls, this.cls);
		super.visit(version, access, AsmUtil.prefixName("Base", name), // name
		            signature, // signature
		            this.prefix("Base", sup), // super
		            ifaces.stream().map(Type::getInternalName).toArray(String[]::new));

		// filter methods and signatures
		// todo annotations, the little fuckers
		// todo and parameters, ffs
		ReflectUtil.getMethods(this.cls).forEach(this::visit);
		ReflectUtil.getFields(this.cls).forEach(this::visit);
		for (Constructor<?> constructor : this.cls.getDeclaredConstructors()) {
			this.visit(constructor);
		}
	}

	public MethodVisitor visit(Constructor constructor) {
		int access = constructor.getModifiers();
		if(!(Modifier.isProtected(access) || Modifier.isPublic(access))) return null;
		String name = "<init>";
		String desc = this.createMethodSignature(null, this.raw(this.reify(constructor.getGenericParameterTypes())), void.class);
		String sign = this.createMethodSignature(null, this.reify(constructor.getGenericParameterTypes()), void.class);
		MethodVisitor visitor = super.visitMethod(access,
		                                          name,
		                                          desc,
		                                          sign,
		                                          null);
		Type[] types = Type.getMethodType(desc).getArgumentTypes();
		for (int i = 0; i < types.length; i++) {
			visitor.visitVarInsn(types[i].getOpcode(ILOAD), i);
		}
		// todo figure this out, maybe super(null, null, null)?
		visitor.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(this.cls), "<init>", desc, false);
		return visitor;
	}


	// for interfaces u don't need reification
	// and declared methods isn't enough for interfaces, try to minimize those
	public MethodVisitor visit(Method method) {
		int access = method.getModifiers();
		if(!Modifier.isProtected(access)) return null;
		String name = method.getName();
		String desc = this.createMethodSignature(null, this.raw(this.reify(method.getGenericParameterTypes())), this.raw(this.reify(method.getGenericReturnType())));
		String sign = this.createMethodSignature(null, this.reify(method.getGenericParameterTypes()), this.reify(method.getGenericReturnType()));
		MethodVisitor visitor = super.visitMethod(access,
		                                          name,
		                                          desc,
		                                          sign,
		                                          null);
		if(!Modifier.isAbstract(access)) {
			visitor.visitMethodInsn(INVOKESTATIC, ImplementationHiddenException.INTERNAL, "create", "()V", false);
		}
		return visitor;
	}

	public FieldVisitor visit(Field field) {
		int access = field.getModifiers();
		String desc = this.toSignature(this.raw(this.reify(field.getType())), false);
		String sign = this.toSignature(this.reify(field.getType()), false);
		if (Modifier.isProtected(access) && !Modifier.isStatic(access) && !this.loader.isMinecraftDesc(desc)) {
			return super.visitField(access, this.name, desc, sign, null);
		}

		// if it's public, the interface abstraction already handled it
		if (Modifier.isProtected(access) && this.loader.isValidClassDesc(desc)) {
			return AsmUtil.generateGetter(super::visitMethod, this.name, access, this.name, desc, sign);
		}

		return null;
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		// technically we should only do this if the class is abstracted, but I'm not sure if it's worth the trouble or not
		// we have to reference this class too
		if (this.name.equals(outerName)) {
			super.visitInnerClass(AsmUtil.prefixName("Base", name), AsmUtil.prefixName("Base", outerName), innerName, access);
		}

		super.visitInnerClass(this.prefix("I", name), this.prefix("I", name), innerName, access);
	}

	@Override
	public void visitOuterClass(String owner, String name, String descriptor) {
		super.visitOuterClass(AsmUtil.prefixName("Base", owner), name, descriptor);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return null;
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		return null;
	}

	@Override
	public Optional<Resource> write() {
		return Optional.of(new Resource() {
			@Override
			String getPath() {
				return ApiAsmBaseAbstracter.this.path;
			}

			@Override
			void write(OutputStream stream) throws IOException {
				stream.write(((ClassWriter) ApiAsmBaseAbstracter.this.cv).toByteArray());
			}
		});
	}

}
