package io.github.f2bb.abstracter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import com.squareup.javapoet.TypeSpec;
import io.github.f2bb.abstracter.ex.InvalidClassException;
import io.github.f2bb.abstracter.util.AbstracterUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public class AbstracterConfig {
	private static final Map<Class<?>, Abstracter<ClassNode>> INTERFACE_API_ASM = new HashMap<>(), INTERFACE_IMPL_ASM 
			                                                                                               =
			                                                                                               new HashMap<>(), BASE_API_ASM = new HashMap<>(), BASE_IMPL_ASM = new HashMap<>();
	private static final Map<Class<?>, Abstracter<TypeSpec.Builder>> INTERFACE_API_JAVA = new HashMap<>(),
			BASE_API_JAVA = new HashMap<>();
	private static final Map<Class<?>, Class<?>[]> INNER_CLASS_OVERRIDES = new HashMap<>();
	private static final Map<Class<?>, Boolean> OUTER_CLASS_OVERRIDES = new HashMap<>();

	/**
	 * 'adds' an inner class to the other class
	 */
	public static void overrideInnerClass(Class<?> cls, Class<?> ...inners) {
		INNER_CLASS_OVERRIDES.put(cls, inners);
		for (Class<?> inner : inners) {
			OUTER_CLASS_OVERRIDES.put(inner, false);
		}
	}

	/**
	 * 'adds' an inner class to the other class
	 */
	public static void overrideInnerClass(String cls, String ...inners) {
		overrideInnerClass(Abstracter.getClass(cls), AbstracterUtil.map(inners, Abstracter::getClass, Class[]::new));
	}

	/**
	 * makes an inner class an outer class
	 */
	public static void overrideOuterClass(Class<?> inner) {
		OUTER_CLASS_OVERRIDES.put(inner, true);
	}

	public static void overrideOuterClass(String inner) {
		overrideOuterClass(Abstracter.getClass(inner));
	}

	public static void registerInterface(String cls,
			Abstracter<ClassNode> interfaceApiAsm,
			Abstracter<ClassNode> interfaceImplAsm,
			Abstracter<TypeSpec.Builder> interfaceApiJava) {
		registerInterface(Abstracter.getClass(cls), interfaceApiAsm, interfaceImplAsm, interfaceApiJava);
	}

	public static void registerBase(String cls,
			Abstracter<ClassNode> baseApiAsm,
			Abstracter<ClassNode> baseImplAsm,
			Abstracter<TypeSpec.Builder> baseApiJava) {
		registerBase(Abstracter.getClass(cls), baseApiAsm, baseImplAsm, baseApiJava);
	}

	public static void registerInterface(String cls,
			Abstracter.Builder<ClassNode> interfaceApiAsm,
			Abstracter.Builder<ClassNode> interfaceImplAsm,
			Abstracter.Builder<TypeSpec.Builder> interfaceApiJava) {
		registerInterface(cls,
				interfaceApiAsm.build(),
				interfaceImplAsm.build(),
				interfaceApiJava.build());
	}

	public static void registerBase(String cls,
			Abstracter.Builder<ClassNode> baseApiAsm,
			Abstracter.Builder<ClassNode> baseImplAsm,
			Abstracter.Builder<TypeSpec.Builder> baseApiJava) {
		registerBase(cls,
				baseApiAsm.build(),
				baseImplAsm.build(),
				baseApiJava.build());
	}

	public static void registerInterface(Class<?> cls,
			Abstracter<ClassNode> interfaceApiAsm,
			Abstracter<ClassNode> interfaceImplAsm,
			Abstracter<TypeSpec.Builder> interfaceApiJava) {
		INTERFACE_API_ASM.put(cls, interfaceApiAsm);
		INTERFACE_IMPL_ASM.put(cls, interfaceImplAsm);
		INTERFACE_API_JAVA.put(cls, interfaceApiJava);
	}

	public static void registerBase(Class<?> cls,
			Abstracter<ClassNode> baseApiAsm,
			Abstracter<ClassNode> baseImplAsm,
			Abstracter<TypeSpec.Builder> baseApiJava) {
		BASE_API_ASM.put(cls, baseApiAsm);
		BASE_IMPL_ASM.put(cls, baseImplAsm);
		BASE_API_JAVA.put(cls, baseApiJava);
	}

	public static void registerInterface(Class<?> cls,
			Abstracter.Builder<ClassNode> interfaceApiAsm,
			Abstracter.Builder<ClassNode> interfaceImplAsm,
			Abstracter.Builder<TypeSpec.Builder> interfaceApiJava) {
		registerInterface(cls,
				interfaceApiAsm.build(),
				interfaceImplAsm.build(),
				interfaceApiJava.build());
	}

	public static void registerBase(Class<?> cls,
			Abstracter.Builder<ClassNode> baseApiAsm,
			Abstracter.Builder<ClassNode> baseImplAsm,
			Abstracter.Builder<TypeSpec.Builder> baseApiJava) {
		registerBase(cls,
				baseApiAsm.build(),
				baseImplAsm.build(),
				baseApiJava.build());
	}


	public static void writeApiJar(ZipOutputStream zos) {
		for (Class<?> cls : INTERFACE_API_ASM.keySet()) {
			writeClass(INTERFACE_API_ASM, zos, cls);
		}

		for (Class<?> cls : BASE_API_ASM.keySet()) {
			writeClass(BASE_API_ASM, zos, cls);
		}
	}

	public static void writeSources(ZipOutputStream zos) {
		writeJava(INTERFACE_API_JAVA, zos);
		writeJava(BASE_API_JAVA, zos);
	}

	public static void writeImplJar(ZipOutputStream zos) {
		for (Class<?> cls : INTERFACE_IMPL_ASM.keySet()) {
			writeClass(INTERFACE_IMPL_ASM, zos, cls);
		}

		for (Class<?> cls : BASE_IMPL_ASM.keySet()) {
			writeClass(BASE_IMPL_ASM, zos, cls);
		}
	}

	private static void writeJava(Map<Class<?>, Abstracter<TypeSpec.Builder>> map, ZipOutputStream out) {
		for (Class<?> cls : map.keySet()) {
			if (OUTER_CLASS_OVERRIDES.computeIfAbsent(cls, c -> {
				Class<?> enclosing = cls.getEnclosingClass();
				return enclosing == null || !map.containsKey(enclosing);
			})) {
				writeJava(map, out, cls);
			}
		}
	}

	private static void writeJava(Map<Class<?>, Abstracter<TypeSpec.Builder>> map, ZipOutputStream zos, Class<?> cls) {
		Abstracter<TypeSpec.Builder> a = map.get(cls);
		TypeSpec.Builder builder = a.apply(cls);
		for (Class<?> c : INNER_CLASS_OVERRIDES.computeIfAbsent(cls, Class::getDeclaredClasses)) {
			Abstracter<TypeSpec.Builder> inner = map.get(c);
			if (inner != null) {
				builder.addType(inner.apply(c).build());
			}
		}
		a.serialize(zos, cls, builder);
	}

	private static void writeClass(Map<Class<?>, Abstracter<ClassNode>> map, ZipOutputStream zos, Class<?> c) {
		Abstracter<ClassNode> a = map.get(c);
		ClassNode node = a.apply(c);
		for (Class<?> cls : INNER_CLASS_OVERRIDES.computeIfAbsent(c, Class::getDeclaredClasses)) {
			if (map.containsKey(cls)) {
				String name = Type.getInternalName(cls);
				int last = name.lastIndexOf('$');
				String simple = name.substring(last + 1);
				node.visitInnerClass(node.name + '$' + simple, node.name, simple, cls.getModifiers());
			}
		}
		System.out.println(c);
		a.serialize(zos, c, node);
	}


	public static boolean isBaseAbstracted(Class<?> cls) {
		return BASE_IMPL_ASM.containsKey(cls);
	}

	public static boolean isInterfaceAbstracted(Class<?> cls) {
		return INTERFACE_IMPL_ASM.containsKey(cls);
	}

	public static String getInterfaceName(Class<?> cls) {
		Abstracter<ClassNode> abstracter = INTERFACE_IMPL_ASM.get(cls);
		if (abstracter != null) {
			return abstracter.nameFunction.toString(cls);
		} else if (Abstracter.isMinecraft(cls)) {
			throw new InvalidClassException(cls);
		} else {
			return Type.getInternalName(cls);
		}
	}

	public static String getBaseName(Class<?> cls) {
		Abstracter<ClassNode> abstracter = BASE_IMPL_ASM.get(cls);
		if (abstracter != null) {
			return abstracter.nameFunction.toString(cls);
		} else if (Abstracter.isMinecraft(cls)) {
			throw new InvalidClassException(cls);
		} else {
			return Type.getInternalName(cls);
		}
	}
}
