package io.github.f2bb.loader;

import java.net.URL;
import java.net.URLClassLoader;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;

public class AbstracterLoaderImpl extends URLClassLoader implements AbstracterLoader {
	private final Remapper remapper = new Remapper() {
		@Override
		public String map(String internalName) {
			Class<?> cls = AbstracterLoaderImpl.this.getClass(Type.getObjectType(internalName).getClassName());
			return AbstracterLoaderImpl.this.getAbstractedName(cls);
		}
	};

	// todo seperate minecraft and classpath?
	public AbstracterLoaderImpl(URL[] urls) {
		super(urls);
	}

	@Override
	public Class<?> getClass(String reflectionName) {
		try {
			return this.loadClass(reflectionName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isMinecraft(Class<?> cls) {
		return cls != null && cls.getClassLoader() == this;
	}

	@Override
	public boolean isAbstracted(Class<?> cls) {
		return cls != null && !cls.isPrimitive() && this.isMinecraft(cls); // todo
	}

	@Override
	public boolean isBaseAbstracted(Class<?> cls) {
		return true;
	}

	@Override
	public Remapper getRemapper() {
		return this.remapper;
	}
}
