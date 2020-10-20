package launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;


public class Launcher {
	private static final Properties MANUAL_MANIFEST;

	static {
		try {
			FileInputStream stream = new FileInputStream("manifest.properties");
			Properties properties = new Properties();
			properties.load(stream);
			stream.close();
			MANUAL_MANIFEST = properties;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// todo unhackify this, prolly just use a fabric project
	public static void main(String[] args) throws MalformedURLException, ClassNotFoundException,
	                                              NoSuchMethodException {
		URL[] urls = new URL[0];
		for (File file : new File("classpath").listFiles()) {
			urls = add(urls, file.toURI().toURL());
		}
		urls = add(urls, new File("fodder.jar").toURI().toURL());
		urls = add(urls, new File("impl.jar").toURI().toURL());
		urls = add(urls,
				new File("C:\\Users\\devan\\Documents\\Java\\f2bb\\F2bb\\Test\\build\\classes\\java\\main\\net").toURI()
				                                                                                                .toURL());

		ClassLoader loader = new URLClassLoader(urls) {
			@Override
			protected Class<?> findClass(String name) throws ClassNotFoundException {
				String internalName = name.replace('.', '/');
				String interfaceName = MANUAL_MANIFEST.getProperty(internalName);
				if (interfaceName != null) {
					InputStream stream = this.getResourceAsStream(internalName + ".class");
					byte[] bytes = addInterface(stream, interfaceName);
					return this.defineClass(name, bytes, 0, bytes.length);
				} else if (internalName.startsWith("devtech/test")) {
					InputStream stream = this.getResourceAsStream(internalName + ".class");
					byte[] bytes = changeName(stream);
					return this.defineClass(name, bytes, 0, bytes.length);
				}
				return super.findClass(name);
			}
		};

		Thread.currentThread().setContextClassLoader(loader);
		Method method = loader.loadClass("devtech.test.Testing").getDeclaredMethod("main", String[].class);
		try {
			method.invoke(null, (Object) args);
		} catch (Throwable t) {
			System.out.println("Ohno!");
			t.printStackTrace();
		}
	}

	public static byte[] changeName(InputStream stream) {
		ClassReader reader = null;
		try {
			reader = new ClassReader(Objects.requireNonNull(stream));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		ClassWriter writer = new ClassWriter(0);
		reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
			@Override
			public void visit(int version,
					int access,
					String name,
					String signature,
					String superName,
					String[] interfaces) {
				super.visit(version, access, name.substring(4), signature, superName, interfaces);
			}
		}, 0);
		return writer.toByteArray();
	}

	public static byte[] addInterface(InputStream stream, String interfaceName) {
		ClassReader reader = null;
		try {
			reader = new ClassReader(Objects.requireNonNull(stream));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		ClassWriter writer = new ClassWriter(0);
		reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
			@Override
			public void visit(int version,
					int access,
					String name,
					String signature,
					String superName,
					String[] interfaces) {
				super.visit(version, access, name, signature, superName, add(interfaces, interfaceName));
			}
		}, 0);
		return writer.toByteArray();
	}

	public static <A> A[] add(A[] as, A a) {
		A[] copy = Arrays.copyOf(as, as.length + 1);
		copy[as.length] = a;
		return copy;
	}
}
