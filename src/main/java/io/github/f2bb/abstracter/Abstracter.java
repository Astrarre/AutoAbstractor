package io.github.f2bb.abstracter;

import java.net.URL;
import java.net.URLClassLoader;

import io.github.f2bb.abstracter.ex.InvalidClassException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SignatureRemapper;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

public class Abstracter implements Opcodes {
	public static final SignatureVisitor EMPTY_VISITOR = new SignatureVisitor(ASM9) {};
	private static final ClsLdr INSTANCE = new ClsLdr();
	public static final Remapper REMAPPER = new Remapper() {
		@Override
		public String map(String internalName) {
			Class<?> cls = Abstracter.getClass(Type.getObjectType(internalName).getClassName());
			return Abstracter.getInterfaceName(cls);
		}
	};

	private static final class ClsLdr extends URLClassLoader {
		public ClsLdr() {
			super(new URL[] {});
		}

		@Override
		public void addURL(URL url) {
			super.addURL(url);
		}
	}

	public static Class<?> getClass(String reflectionName) {
		try {
			return INSTANCE.loadClass(reflectionName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getInterfaceName(Class<?> cls) {
		return getInterfaceName(cls, Type.getInternalName(cls));
	}

	public static String getInterfaceDesc(Class<?> cls) {
		return getInterfaceName(cls, Type.getDescriptor(cls));
	}

	public static boolean isMinecraft(Class<?> cls) {
		return cls.getClassLoader() == INSTANCE;
	}

	public static boolean isBaseAbstracted(Class<?> cls) {
		return isMinecraft(cls) && cls.getSimpleName().contains("Block"); // todo
	}

	public static boolean isAbstracted(Class<?> cls) {
		return isMinecraft(cls) && isAbstractedInternal(cls);
	}

	/**
	 * @return true if the class is a minecraft class, but isn't supposed to be abstracted
	 */
	public static boolean isUnabstractedClass(Class<?> cls) {
		return isMinecraft(cls) && !isAbstractedInternal(cls);
	}

	public static boolean isValid(String signature) {
		SignatureRemapper remapper = new SignatureRemapper(EMPTY_VISITOR, REMAPPER);
		SignatureReader reader = new SignatureReader(signature);
		try {
			reader.accept(remapper);
			return true;
		} catch (InvalidClassException e) {
			return false;
		}
	}

	private static String getInterfaceName(Class<?> cls, String str) {
		if (isAbstracted(cls)) {
			int last = str.lastIndexOf('/') + 1;
			return str.substring(0, last) + "I" + str.substring(last);
		} else if (isMinecraft(cls)) {
			throw new InvalidClassException(cls);
		}
		return str;
	}


	private static boolean isAbstractedInternal(Class<?> cls) {
		return cls.getSimpleName().contains("Block"); // todo
	}
}
