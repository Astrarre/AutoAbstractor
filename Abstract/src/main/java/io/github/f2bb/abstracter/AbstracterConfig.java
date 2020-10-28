package io.github.f2bb.abstracter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipOutputStream;

import com.squareup.javapoet.TypeSpec;
import io.github.f2bb.abstracter.ex.InvalidClassException;
import io.github.f2bb.abstracter.func.abstracting.constructor.ConstructorAbstracter;
import io.github.f2bb.abstracter.func.abstracting.field.FieldAbstracter;
import io.github.f2bb.abstracter.func.abstracting.method.MethodAbstracter;
import io.github.f2bb.abstracter.func.elements.ConstructorSupplier;
import io.github.f2bb.abstracter.func.elements.FieldSupplier;
import io.github.f2bb.abstracter.func.elements.MethodSupplier;
import io.github.f2bb.abstracter.func.inheritance.InterfaceFunction;
import io.github.f2bb.abstracter.func.inheritance.SuperFunction;
import io.github.f2bb.abstracter.func.serialization.SerializingFunction;
import io.github.f2bb.abstracter.util.AbstracterLoader;
import io.github.f2bb.abstracter.util.ArrayUtil;
import io.github.f2bb.abstracter.util.asm.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public class AbstracterConfig implements Opcodes {
	private static final Map<Class<?>, Abstracter> INTERFACE_ABSTRACTION = new HashMap<>(), BASE_ABSTRACTION =
			                                                                                        new HashMap<>();
	private static final Map<Class<?>, Class<?>[]> INNER_CLASS_OVERRIDES = new HashMap<>();
	private static final Map<Class<?>, Boolean> IS_INNER = new HashMap<>();

	public static void writeManifest(OutputStream stream) throws IOException {
		Properties properties = new Properties();
		INTERFACE_ABSTRACTION
				.forEach((c, a) -> properties.setProperty(Type.getInternalName(c), a.nameFunction.toString(c)));
		properties.store(stream, "F2bb Interface Manifest");
		// todo store licence or something
	}

	public static void writeJar(ZipOutputStream out, boolean impl) {
		write(out, INTERFACE_ABSTRACTION, impl);
		write(out, BASE_ABSTRACTION, impl);
	}

	public static void write(ZipOutputStream out, Map<Class<?>, Abstracter> abstraction, boolean impl) {
		Map<Class<?>, ClassNode> cache = new HashMap<>();
		abstraction.forEach((cls, abs) -> {
			abs.serialize(out, cls, cache.computeIfAbsent(cls, c -> asm(abstraction, cache, c, impl)));
			if (!IS_INNER.getOrDefault(cls, cls.getEnclosingClass() != null) && !impl) {
				abs.serialize(out, cls, java(abstraction, cls, false));
			}
		});
	}

	public static ClassNode asm(Map<Class<?>, Abstracter> map,
			Map<Class<?>, ClassNode> cache,
			Class<?> cls,
			boolean impl) {
		ClassNode node = map.get(cls).applyAsm(cls, impl);
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

	public static TypeSpec.Builder java(Map<Class<?>, Abstracter> map, Class<?> cls, boolean impl) {
		TypeSpec.Builder node = map.get(cls).applyJava(cls, impl);
		if (node != null) {
			for (Class<?> inner : INNER_CLASS_OVERRIDES.getOrDefault(cls, cls.getClasses())) {
				if (map.containsKey(inner)) {
					node.addType(java(map, inner, impl).build());
				}
			}
		}
		return node;
	}

	public static void manualInterface(String mcClass, String abstraction) {
		String internal = abstraction.replace('.', '/');
		registerInterface(mcClass,
				new Abstracter((access, name, variables, sup, interfaces) -> null,
						(access, name, variables, sup, interfaces) -> null,
						ConstructorSupplier.EMPTY,
						FieldSupplier.EMPTY,
						MethodSupplier.EMPTY,
						InterfaceFunction.EMPTY,
						SuperFunction.EMPTY,
						instance -> internal,
						operand -> operand,
						FieldAbstracter.nothing(),
						MethodAbstracter.nothing(),
						ConstructorAbstracter.nothing(),
						SerializingFunction.nothing(),
						FieldAbstracter.nothing(),
						MethodAbstracter.nothing(),
						ConstructorAbstracter.nothing(),
						SerializingFunction.nothing()));
	}

	public static void registerInnerOverride(String cls, String... inners) {
		Class<?>[] innerClasses = ArrayUtil.map(inners, AbstracterLoader::getClass, Class[]::new);
		for (Class<?> aClass : innerClasses) {
			IS_INNER.put(aClass, true);
		}
		INNER_CLASS_OVERRIDES.put(AbstracterLoader.getClass(cls), innerClasses);
	}

	public static void makeOuter(String cls) {
		IS_INNER.put(AbstracterLoader.getClass(cls), false);
	}

	public static void registerInterface(String cls, Abstracter abstracter) {
		INTERFACE_ABSTRACTION.put(AbstracterLoader.getClass(cls), abstracter);
	}

	public static void registerBase(String cls, Abstracter abstracter) {
		BASE_ABSTRACTION.put(AbstracterLoader.getClass(cls), abstracter);
	}

	public static boolean isInterfaceAbstracted(Class<?> cls) {
		return INTERFACE_ABSTRACTION.containsKey(cls);
	}

	public static String getInterfaceName(Class<?> cls) {
		Abstracter function = INTERFACE_ABSTRACTION.get(cls);
		if (function == null) {
			if (AbstracterLoader.isMinecraft(cls)) {
				throw new InvalidClassException(cls);
			} else {
				return Type.getInternalName(cls);
			}
		}

		return function.nameFunction.toString(cls);
	}

	public static String getBaseName(Class<?> cls) {
		return BASE_ABSTRACTION.get(cls).nameFunction.toString(cls);
	}
}
