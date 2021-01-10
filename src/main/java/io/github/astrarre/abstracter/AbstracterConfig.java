package io.github.astrarre.abstracter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import io.github.astrarre.abstracter.abs.ManualAbstracter;
import io.github.astrarre.abstracter.ex.InvalidClassException;
import io.github.astrarre.abstracter.util.AbstracterLoader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public class AbstracterConfig implements Opcodes {
	// isolated classloader
	public static final AbstracterLoader CLASSPATH = new AbstracterLoader(ClassLoader.getSystemClassLoader().getParent());
	public static final AbstracterLoader INSTANCE = new AbstracterLoader(CLASSPATH);
	private static final Map<String, AbstractAbstracter> INTERFACE_ABSTRACTION = new HashMap<>();
	private static final Map<String, AbstractAbstracter> BASE_ABSTRACTION = new HashMap<>();

	public static void writeManifest(OutputStream stream) throws IOException {
		Properties properties = new Properties();
		INTERFACE_ABSTRACTION.forEach((c, a) -> properties.setProperty(c, a.name));
		properties.store(stream, "F2bb Interface Manifest");
		// todo store licence or something
		// todo remap
	}
	public static void writeJar(ZipOutputStream out, boolean impl) throws IOException {
		write(out, INTERFACE_ABSTRACTION, impl);
		write(out, BASE_ABSTRACTION, impl);
		if (impl) {
			out.putNextEntry(new ZipEntry("intr_manifest.properties"));
			writeManifest(out);
			out.closeEntry();
		}
	}

	private static void write(ZipOutputStream out, Map<String, AbstractAbstracter> abstraction, boolean impl) {
		Map<String, ClassNode> cache = new HashMap<>();
		abstraction.forEach((cls, abs) -> {
			ClassNode node = cache.computeIfAbsent(cls, c -> abstraction.get(c).apply(impl));
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			node.accept(writer);
			try {
				out.putNextEntry(new ZipEntry(node.name + ".class"));
				out.write(writer.toByteArray());
				out.closeEntry();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	public static void manualInterface(Class<?> mcClass, String abstraction) {
		registerInterface(new ManualAbstracter(mcClass, abstraction));
	}

	public static AbstractAbstracter registerInterface(AbstractAbstracter abstracter) {
		INTERFACE_ABSTRACTION.put(Type.getInternalName(abstracter.getCls()), abstracter);
		return abstracter;
	}

	public static AbstractAbstracter getInterfaceAbstraction(String internalName) {
		return INTERFACE_ABSTRACTION.get(internalName);
	}

	public static AbstractAbstracter registerBase(AbstractAbstracter abstracter) {
		BASE_ABSTRACTION.put(Type.getInternalName(abstracter.getCls()), abstracter);
		return abstracter;
	}

	public static Class<?> getClass(String internalName) {
		try {
			return Class.forName(internalName.replace('/', '.'), false, INSTANCE);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static String getInterfaceName(Class<?> cls) {
		return getInterfaceName(Type.getInternalName(cls));
	}

	public static String getInterfaceName(String cls) {
		AbstractAbstracter abstraction = INTERFACE_ABSTRACTION.get(cls);
		if (abstraction == null) {
			Class<?> c = getClass(cls);
			if (isMinecraft(c)) {
				throw new InvalidClassException(c);
			} else {
				return cls;
			}
		}

		return abstraction.name;
	}

	public static boolean isMinecraft(Class<?> cls) {
		return cls != null && cls.getClassLoader() == INSTANCE;
	}

	/**
	 * @return abstracted class name -> minecraft class name
	 */
	public static Map<String, String> nameMap() {
		Map<String, String> map = new HashMap<>();
		BASE_ABSTRACTION.forEach((k, a) -> map.put(k, a.name));
		INTERFACE_ABSTRACTION.forEach((k, a) -> map.put(k, a.name));
		return map;
	}

	/**
	 * @return true if the class is a minecraft class, but isn't supposed to be abstracted
	 */
	public static boolean isUnabstractedClass(Class<?> cls) {
		return isMinecraft(cls) && !isInterfaceAbstracted(cls);
	}

	public static boolean isInterfaceAbstracted(Class<?> cls) {
		return isInterfaceAbstracted(Type.getInternalName(cls));
	}

	public static boolean isInterfaceAbstracted(String internalName) {
		return INTERFACE_ABSTRACTION.containsKey(internalName);
	}
}
