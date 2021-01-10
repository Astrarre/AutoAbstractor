package io.github.astrarre.abstracter.abs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import io.github.astrarre.abstracter.abs.method.MethodAbstracter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;

public class ManualAbstracter extends AbstractAbstracter {
	public ManualAbstracter(Class<?> cls, String name) {
		super(cls, name, null, null, null, null, null);
	}

	@Override
	public ClassNode apply(boolean impl) {
		return null;
	}

	@Override
	public int getAccess(int modifiers) {return 0;}

	@Override
	public MethodAbstracter<Constructor<?>> abstractConstructor(Constructor<?> constructor, boolean impl) {return null;}

	@Override
	public void abstractField(ClassNode node, Field field, boolean impl) {}

	@Override
	public MethodAbstracter abstractMethod(Method method, boolean impl) {
		return null;
	}

	@Override
	public void postProcess(ClassNode node, boolean impl) {}

	@Override
	public void castToMinecraft(MethodVisitor visitor, Consumer<MethodVisitor> apply, Location parameter) {

	}

	@Override
	public void castToCurrent(MethodVisitor visitor, Consumer<MethodVisitor> apply, Location parameter) {

	}
}
