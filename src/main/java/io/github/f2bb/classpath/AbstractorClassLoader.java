package io.github.f2bb.classpath;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureWriter;

public class AbstractorClassLoader extends URLClassLoader {
	public static AbstractorClassLoader create(URL[] abstraction, URL[] classpath) {
		return new AbstractorClassLoader(new URLClassLoader(abstraction), classpath);
	}
	private final AbstractorRemapper remapper = new AbstractorRemapper(this);
	private final ClassLoader minecraft;

	public AbstractorClassLoader(ClassLoader loader, URL[] urls) {
		super(urls, loader);
		this.minecraft = loader;
	}

	public String remap(String sign) {
		SignatureReader reader = new SignatureReader(sign);
		SignatureWriter writer = new SignatureWriter();
		reader.accept(this.remapper.createSignatureRemapper(writer));
		return writer.toString();
	}

	public boolean isMinecraftDesc(String desc) {
		return this.isMinecraft(Type.getType(desc).getClassName());
	}

	public boolean isMinecraft(String reflectionName) {
		try {
			return this.isMinecraft(this.loadClass(reflectionName));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isValidClass(Class<?> cls) {
		return cls == null || !this.isMinecraft(cls) || this.canAbstractClass(cls);
	}


	public boolean canAbstractClass(Class<?> cls) {
		return true;
	}

	public boolean isValidClassDesc(String desc) {
		return true;
	}

	// todo check for any references to unabstracted classes
	// todo filter out private and package-private
	public boolean canAbstractField(Field field) {
		return true;
	}


	public boolean canAbstractMethod(Method method) {
		return true;
	}

	public boolean canAbstractConstructor(Constructor<?> constructor) {
		return true;
	}

	public ClassLoader getMinecraft() {
		return this.minecraft;
	}

	public AbstractorRemapper getRemapper() {
		return this.remapper;
	}

	public boolean isMinecraft(Class<?> type) {
		return type.getClassLoader() == this.minecraft;
	}

	@Override
	public void addURL(URL url) {
		super.addURL(url);
	}

}
