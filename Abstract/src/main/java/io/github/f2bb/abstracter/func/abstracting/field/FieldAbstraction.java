package io.github.f2bb.abstracter.func.abstracting.field;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class FieldAbstraction implements Opcodes {
	public static String getEtterName(String prefix, Class<?> desc, String name) {
		return prefix + name(Type.getDescriptor(desc)) + Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	public static String getEtterName(String prefix, String desc, String name) {
		return prefix + name(desc) + Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	private static String name(String desc) {
		Type type = Type.getType(desc);
		switch (type.getSort()) {
		case Type.ARRAY:
			return "Arr";
		case Type.OBJECT:
			return "Obj";
		case Type.BYTE:
			return "Byte";
		case Type.BOOLEAN:
			return "Bool";
		case Type.SHORT:
			return "Short";
		case Type.CHAR:
			return "Char";
		case Type.INT:
			return "Int";
		case Type.FLOAT:
			return "Float";
		case Type.LONG:
			return "Long";
		case Type.DOUBLE:
			return "Double";
		}
		throw new IllegalArgumentException(desc);
	}
}
