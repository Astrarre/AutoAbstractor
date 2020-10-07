package io.github.f2bb.abstraction;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import io.github.f2bb.classpath.AbstractorClassLoader;
import io.github.f2bb.reflect.ReifiedType;
import io.github.f2bb.util.AsmUtil;
import io.github.f2bb.util.ReflectUtil;
import io.github.f2bb.util.Util;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

// todo extention methods
public abstract class AbstractBaseAbstracter extends AbstractAbstracter {
	protected String name;
	public AbstractBaseAbstracter(ClassVisitor classVisitor, AbstractorClassLoader loader, Class<?> cls) {
		super(classVisitor, loader, cls);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.name = name;
		Class<?> sup = this.getValidSuper();
		Set<ReifiedType> reifiedInterfaces = this.reify(this.getInterfaces(this.cls));
		// todo add "this" to interfaces, make a fake reified type class or smn
		this.visit(version, access, name, this.cls.getTypeParameters(), new ReifiedType(this.reify(sup), sup), reifiedInterfaces);
		ReflectUtil.getMethods(this.cls).stream().filter(this.loader::canAbstractMethod).forEach(m -> this.visit(m, m.getTypeParameters(), this.reify(m.getGenericParameterTypes()), this.reify(m.getGenericReturnType())));
		ReflectUtil.getFields(this.cls).stream().filter(this.loader::canAbstractField).filter(f -> Modifier.isProtected(f.getModifiers())).forEach(f -> this.visit(f, this.reify(f.getGenericType())));
		Arrays.stream(this.cls.getDeclaredConstructors()).filter(this.loader::canAbstractConstructor).forEach(c -> this.visit(c, c.getTypeParameters(), this.reify(c.getGenericParameterTypes())));
	}

	public abstract void visit(int version, int access, String name, TypeVariable<?>[] typeVariables, ReifiedType superClass, Collection<ReifiedType> interfaces);

	public abstract void visit(Constructor constructor, TypeVariable<?>[] variable, Type[] parameters);
	public abstract void visit(Method method, TypeVariable<?>[] variable, Type[] parameters, Type returnType);
	public abstract void visit(Field field, Type type);

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
	// only for asm
}
