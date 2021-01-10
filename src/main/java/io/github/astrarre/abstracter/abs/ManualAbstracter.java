package io.github.astrarre.abstracter.abs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.abs.field.FieldAbstracter;
import io.github.astrarre.abstracter.abs.method.MethodAbstracter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;

public class ManualAbstracter extends AbstractAbstracter {
	public ManualAbstracter(Class<?> cls, String name) {
		super(cls, name, null, null, null, null, null);
	}

	@Override
	public ClassNode apply(AbstracterConfig config, boolean impl) {
		return null;
	}

	@Override
	public int getAccess(AbstracterConfig config, int modifiers) {return 0;}

	@Override
	public MethodAbstracter<Constructor<?>> abstractConstructor(AbstracterConfig config, Constructor<?> constructor, boolean impl) {return null;}

	@Override
	public MethodAbstracter<Method> abstractMethod(AbstracterConfig config, Method method, boolean impl) {
		return null;
	}

	@Override
	public FieldAbstracter abstractField(AbstracterConfig config, Field field, boolean impl) {
		return null;
	}

	@Override
	public void postProcess(AbstracterConfig config, ClassNode node, boolean impl) {}

	@Override
	public void castToMinecraft(MethodVisitor visitor, Consumer<MethodVisitor> apply, Location parameter) {

	}

	@Override
	public void castToCurrent(MethodVisitor visitor, Consumer<MethodVisitor> apply, Location parameter) {

	}
}
