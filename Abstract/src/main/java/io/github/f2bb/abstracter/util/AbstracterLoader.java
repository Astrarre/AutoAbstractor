package io.github.f2bb.abstracter.util;

import java.net.URL;
import java.net.URLClassLoader;

import io.github.f2bb.abstracter.AbstracterConfig;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;

public class AbstracterLoader extends URLClassLoader {
	public static final AbstracterLoader CLASSPATH = new AbstracterLoader();
	public static final AbstracterLoader INSTANCE = new AbstracterLoader(CLASSPATH);

	public AbstracterLoader() {
		super(new URL[] {});
	}

	public AbstracterLoader(ClassLoader parent) {
		super(new URL[] {}, parent);
	}

	public static boolean isMinecraft(Class<?> cls) {
		return cls != null && cls.getClassLoader() == INSTANCE;
	}

	public static Class<?> getClass(String reflectionName) {
		try {
			return INSTANCE.loadClass(reflectionName);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * @return true if the class is a minecraft class, but isn't supposed to be abstracted
	 */
	public static boolean isUnabstractedClass(Class<?> cls) {
		return isMinecraft(cls) && !AbstracterConfig.isInterfaceAbstracted(cls);
	}

	@Override
	public void addURL(URL url) {
		super.addURL(url);
	}
}
