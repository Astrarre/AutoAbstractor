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
import io.github.astrarre.abstracter.util.ArrayUtil;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

@SuppressWarnings ("unchecked")
public class AbstracterConfig implements Opcodes {
	/**
	 * describes how to go from one kind of class to another (api -> minecraft) or (minecraft -> api)
	 *
	 * the map goes from the desired class to the casting function
	 */
	public static final Map<String, CastingFunction> TRANSLATION = new HashMap<>();
	// isolated classloader
	public static final AbstracterLoader CLASSPATH = new AbstracterLoader(ClassLoader.getSystemClassLoader().getParent());
	public static final AbstracterLoader INSTANCE = new AbstracterLoader(CLASSPATH);
	private static final Map<Class<?>, AbstractAbstracter> INTERFACE_ABSTRACTION = new HashMap<>();
	private static final Map<Class<?>, AbstractAbstracter> BASE_ABSTRACTION = new HashMap<>();

	// todo to Abstracter
	private static final Map<Class<?>, Class<?>[]> INNER_CLASS_OVERRIDES = new HashMap<>();

	public static void writeManifest(OutputStream stream) throws IOException {
		Properties properties = new Properties();
		INTERFACE_ABSTRACTION.forEach((c, a) -> properties.setProperty(Type.getInternalName(c), a.name));
		properties.store(stream, "F2bb Interface Manifest");
		// todo store licence or something
		// todo remap
	}

	public static void writeBaseManifest(OutputStream stream) throws IOException {
		Properties properties = new Properties();
		BASE_ABSTRACTION.forEach((c, a) -> properties.setProperty(Type.getInternalName(c), a.name));
		properties.store(stream, "F2bb Base Manifest");
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

	private static void write(ZipOutputStream out, Map<Class<?>, AbstractAbstracter> abstraction, boolean impl) {
		Map<Class<?>, ClassNode> cache = new HashMap<>();
		abstraction.forEach((cls, abs) -> {
			ClassNode node = cache.computeIfAbsent(cls, c -> asm(abstraction, cache, c, impl));
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

	private static ClassNode asm(Map<Class<?>, AbstractAbstracter> map, Map<Class<?>, ClassNode> cache, Class<?> cls, boolean impl) {
		ClassNode node = map.get(cls).apply(impl);
		if (node != null) {
			for (Class<?> inner : INNER_CLASS_OVERRIDES.getOrDefault(cls, cls.getClasses())) {
				if (map.containsKey(inner)) {
					ClassNode innerNode = cache.computeIfAbsent(inner, c -> asm(map, cache, c, impl));
					node.visitInnerClass(innerNode.name, node.name, AbstractAbstracter.getInnerName(innerNode.name), innerNode.access);
				}
			}
		}
		return node;
	}

	public static void manualInterface(Class<?> mcClass, String abstraction) {
		registerInterface(new ManualAbstracter(mcClass, abstraction));
	}

	public static AbstractAbstracter registerInterface(AbstractAbstracter abstracter) {
		INTERFACE_ABSTRACTION.put(abstracter.getCls(), abstracter);
		return abstracter;
	}

	public static AbstractAbstracter registerEnum(AbstractAbstracter abstracter) {
		INTERFACE_ABSTRACTION.put(abstracter.getCls(), abstracter);
		return abstracter;
	}

	public static AbstractAbstracter registerBase(AbstractAbstracter abstracter) {
		BASE_ABSTRACTION.put(abstracter.getCls(), abstracter);
		return abstracter;
	}

	public static void registerInnerOverride(Class<?> cls, Class<?>... inners) {
		registerInnerOverride(cls.getName(), ArrayUtil.map(inners, Class::getName, String[]::new));
	}

	public static void registerInnerOverride(String cls, String... inners) {
		Class<?>[] innerClasses = ArrayUtil.map(inners, AbstracterConfig::getClass, Class[]::new);
		INNER_CLASS_OVERRIDES.put(getClass(cls), innerClasses);
	}

	public static Class<?> getClass(String reflectionName) {
		try {
			return INSTANCE.loadClass(reflectionName);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public static String getInterfaceName(Class<?> cls) {
		AbstractAbstracter abstraction = INTERFACE_ABSTRACTION.get(cls);
		if (abstraction == null) {
			if (isMinecraft(cls)) {
				throw new InvalidClassException(cls);
			} else {
				return Type.getInternalName(cls);
			}
		}

		return abstraction.name;
	}

	public static boolean isMinecraft(Class<?> cls) {
		return cls != null && cls.getClassLoader() == INSTANCE;
	}

	public static String getBaseName(Class<?> cls) {
		return BASE_ABSTRACTION.get(cls).name;
	}

	/**
	 * @return abstracted class name -> minecraft class name
	 */
	public static Map<String, String> nameMap() {
		Map<String, String> map = new HashMap<>();
		BASE_ABSTRACTION.forEach((k, a) -> map.put(Type.getInternalName(k), a.name));
		INTERFACE_ABSTRACTION.forEach((k, a) -> map.put(Type.getInternalName(k), a.name));
		return map;
	}

	/**
	 * @return true if the class is a minecraft class, but isn't supposed to be abstracted
	 */
	public static boolean isUnabstractedClass(Class<?> cls) {
		return isMinecraft(cls) && !isInterfaceAbstracted(cls);
	}

	public static boolean isInterfaceAbstracted(Class<?> cls) {
		return INTERFACE_ABSTRACTION.containsKey(cls);
	}

	public interface CastingFunction {
		CastingFunction DEFAULT = (c, d, v) -> v.visitTypeInsn(CHECKCAST, d);

		void accept(String currentType, String desiredType, MethodVisitor visitor);
	}
}
