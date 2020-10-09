package io.github.f2bb;

import io.github.f2bb.util.AsmUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SignatureRemapper;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.tree.ClassNode;

public interface Abstracter extends Opcodes {
	ClassNode getClass(String internalName);

	/**
	 * @return true if the class came from the minecraft jar
	 */
	boolean isMinecraft(String internalName);

	/**
	 * @return true if the class is interface abstracted
	 */
	boolean isInterfaceAbstracted(String internalName);

	boolean shouldAbstractMethod(String clsName, String name, String desc);

	boolean shouldAbstractField(String clsName, String name, String desc);

	default boolean isValid(Type type) {
		int sort = type.getSort();
		if (sort == Type.OBJECT) {
			return this.isInterfaceAbstracted(type.getInternalName());
		} else if (sort == Type.ARRAY) {
			return this.isValid(type.getElementType());
		}
		return true;
	}

	default Remapper getRemapper() {
		return new Remapper() {
			@Override
			public String mapType(String internalName) {
				if (Abstracter.this.isInterfaceAbstracted(internalName)) {
					return AsmUtil.prefixName("I", internalName);
				}

				if (!Abstracter.this.isMinecraft(internalName)) {
					throw new IllegalArgumentException("Invalid class found in signature: " + internalName);
				}
				return super.mapType(internalName);
			}
		};
	}

	default String remap(String sign) {
		SignatureWriter writer = new SignatureWriter();
		SignatureRemapper remapper = new SignatureRemapper(writer, this.getRemapper());
		SignatureReader reader = new SignatureReader(sign);
		reader.accept(remapper);
		return writer.toString();
	}

	default boolean shouldAbstractSign(String desc, String sign) {
		if (sign == null) {
			sign = desc;
		}
		SignatureRemapper remapper = new SignatureRemapper(null, this.getRemapper());
		SignatureReader reader = new SignatureReader(sign);
		try {
			reader.accept(remapper);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}
