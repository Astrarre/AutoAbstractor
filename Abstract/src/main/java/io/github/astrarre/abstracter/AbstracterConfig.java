package io.github.astrarre.abstracter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.github.astrarre.abstracter.abs.ManualAbstracter;
import io.github.astrarre.abstracter.ex.InvalidClassException;
import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import io.github.astrarre.abstracter.util.AbstracterLoader;
import io.github.astrarre.abstracter.util.ArrayUtil;
import io.github.astrarre.abstracter.util.reflect.TypeUtil;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

@SuppressWarnings ("unchecked")
public class AbstracterConfig implements Opcodes {
	private static final Map<Class<?>, AbstractAbstracter> INTERFACE_ABSTRACTION = new HashMap<>();
	private static final Map<Class<?>, AbstractAbstracter> BASE_ABSTRACTION = new HashMap<>();
	private static final Map<Class<?>, AbstractAbstracter> CONSTANT_ABSTRACTIONS = new HashMap<>();

	private static final Map<Class<?>, Class<?>[]> INNER_CLASS_OVERRIDES = new HashMap<>();

	public static void writeManifest(OutputStream stream) throws IOException {
		Properties properties = new Properties();
		INTERFACE_ABSTRACTION.forEach((c, a) -> properties.setProperty(Type.getInternalName(c), a.name));
		properties.store(stream, "F2bb Interface Manifest");
		// todo store licence or something
	}

	public static void writeJar(ZipOutputStream out, boolean impl) {
		write(out, INTERFACE_ABSTRACTION, impl);
		write(out, BASE_ABSTRACTION, impl);
		write(out, CONSTANT_ABSTRACTIONS, impl);
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

	private static ClassNode asm(Map<Class<?>, AbstractAbstracter> map,
			Map<Class<?>, ClassNode> cache,
			Class<?> cls,
			boolean impl) {
		ClassNode node = map.get(cls).apply(impl);
		if (node != null) {
			for (Class<?> inner : INNER_CLASS_OVERRIDES.getOrDefault(cls, cls.getClasses())) {
				if (map.containsKey(inner)) {
					ClassNode innerNode = cache.computeIfAbsent(inner, c -> asm(map, cache, c, impl));
					node.visitInnerClass(innerNode.name,
							node.name,
							TypeUtil.getInnerName(innerNode.name),
							innerNode.access);
				}
			}
		}
		return node;
	}

	public static void manualInterface(Class<?> mcClass, String abstraction) {
		registerInterface(mcClass, c -> new ManualAbstracter(c, abstraction));
	}

	public static void manualInterface(String mcClass, String abstraction) {
		registerInterface(mcClass, c -> new ManualAbstracter(c, abstraction));
	}

	public static void registerInterface(String cls, Function<Class<?>, AbstractAbstracter> abstracter) {
		Class<?> c = AbstracterLoader.getClass(cls);
		INTERFACE_ABSTRACTION.put(c, abstracter.apply(c));
	}

	public static void registerInterface(Class<?> cls, Function<Class<?>, AbstractAbstracter> abstracter) {
		registerInterface(cls.getName(), abstracter);
	}

	public static void registerBase(Class<?> cls, Function<Class<?>, AbstractAbstracter> abstracter) {
		registerBase(cls.getName(), abstracter);
	}

	public static void registerBase(String cls, Function<Class<?>, AbstractAbstracter> abstracter) {
		Class<?> c = AbstracterLoader.getClass(cls);
		BASE_ABSTRACTION.put(c, abstracter.apply(c));
	}

	public static void registerConstants(String cls, Function<Class<?>, AbstractAbstracter> abstracter) {
		Class<?> c = AbstracterLoader.getClass(cls);
		CONSTANT_ABSTRACTIONS.put(c, abstracter.apply(c));
	}

	public static void registerConstants(Class<?> cls, Function<Class<?>, AbstractAbstracter> abstracter) {
		registerConstants(cls.getName(), abstracter);
	}

	public static void registerInnerOverride(Class<?> cls, Class<?>... inners) {
		registerInnerOverride(cls.getName(), ArrayUtil.map(inners, Class::getName, String[]::new));
	}

	public static void registerInnerOverride(String cls, String... inners) {
		Class<?>[] innerClasses = ArrayUtil.map(inners, AbstracterLoader::getClass, Class[]::new);
		INNER_CLASS_OVERRIDES.put(AbstracterLoader.getClass(cls), innerClasses);
	}

	public static boolean isInterfaceAbstracted(Class<?> cls) {
		return INTERFACE_ABSTRACTION.containsKey(cls);
	}

	public static String getInterfaceName(Class<?> cls) {
		AbstractAbstracter abstraction = INTERFACE_ABSTRACTION.get(cls);
		if (abstraction == null) {
			if (AbstracterLoader.isMinecraft(cls)) {
				throw new InvalidClassException(cls);
			} else {
				return Type.getInternalName(cls);
			}
		}

		return abstraction.name;
	}

	public static String getBaseName(Class<?> cls) {
		return BASE_ABSTRACTION.get(cls).name;
	}

	/**
	 * @return abstracted class name -> minecraft class name
	 */
	public static Map<String, String> nameMap() {
		Map<String, String> map = new HashMap<>();
		BASE_ABSTRACTION.forEach((k, a) -> map.put(a.name, Type.getInternalName(k)));
		INTERFACE_ABSTRACTION.forEach((k, a) -> map.put(a.name, Type.getInternalName(k)));
		CONSTANT_ABSTRACTIONS.forEach((k, a) -> map.put(a.name, Type.getInternalName(k)));
		return map;
	}
}
