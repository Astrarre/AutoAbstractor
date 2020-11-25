package io.github.astrarre.abstracter.abs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
	public int getAccess() {return 0;}

	@Override
	public void abstractField(ClassNode node, Field field, boolean impl) {}

	@Override
	public void abstractMethod(ClassNode node, Method method, boolean impl) {}

	@Override
	public void abstractConstructor(ClassNode node, Constructor<?> constructor, boolean impl) {}

	@Override
	public void postProcess(ClassNode node, boolean impl) {}
}
