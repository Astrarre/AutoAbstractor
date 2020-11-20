package pkg.test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Properties;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class Loader extends URLClassLoader {
	private static final ClassLoader LOADER = Loader.class.getClassLoader();
	private static final Map<String, String> ATTACH;

	static {
		try {
			Properties properties = new Properties();
			properties.load(new FileReader("manifest.properties"));
			ATTACH = (Map) properties;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Loader() throws MalformedURLException {
		super(new URL[] {
				new File("fodder.jar").toURI().toURL(),
				new File("impl.jar").toURI().toURL()
		});
		for (File file : new File("classpath").listFiles()) {
			this.addURL(file.toURI().toURL());
		}
	}

	public static void main(String[] args)
			throws MalformedURLException, NoSuchMethodException, InvocationTargetException, IllegalAccessException,
			       ClassNotFoundException {
		Loader loader = new Loader();
		Thread.currentThread().setContextClassLoader(loader);
		loader.findClass("pkg.test.Test").getDeclaredMethod("main").invoke(null);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		if (name.startsWith("pkg.test")) {
			try {
				return this.locate(name);
			} catch (Throwable e) {
				return super.findClass(name);
			}
		} else {
			try {
				return getParent().loadClass(name);
			} catch (Throwable e) {
				try {
					return this.locate(name);
				} catch (IOException ex) {
					throw new ClassNotFoundException();
				}
			}
		}
	}

	private Class<?> locate(String name) throws IOException {
		String internal = name.replace('.', '/');
		InputStream stream = (name.equals("pkg.Test") ? LOADER : this).getResourceAsStream(internal + ".class");
		ClassReader reader = new ClassReader(stream);
		ClassNode node = new ClassNode();
		reader.accept(node, 0);

		String attach = ATTACH.get(internal);
		if (attach != null) {
			node.interfaces.add(attach);
		}

		ClassWriter writer = new ClassWriter(0);
		node.accept(writer);
		byte code[] = writer.toByteArray();
		return this.defineClass(name, code, 0, code.length);
	}

}
