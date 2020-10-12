package io.github.f2bb.loader;

import io.github.f2bb.ex.InvalidClassException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SignatureRemapper;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

public interface AbstracterLoader extends Opcodes {
	Class<?> getClass(String reflectionName);

	default boolean isMinecraft(String reflective) {
		return this.isMinecraft(this.getClass(reflective));
	}

	/**
	 * true if the class is a minecraft class
	 */
	boolean isMinecraft(Class<?> cls);

	/**
	 * @return true if the class will be interface abstracted
	 */
	boolean isAbstracted(Class<?> cls);

	/**
	 * @return true if the class will be base abstracted
	 */
	boolean isBaseAbstracted(Class<?> cls);

	Remapper getRemapper();

	default String getAbstractedName(Class<?> cls) {
		if(this.isAbstracted(cls)) {
			String desc = Type.getInternalName(cls);
			int last = desc.lastIndexOf('/')+1;
			return desc.substring(0, last) + "I" + desc.substring(last);
		} else if(this.isMinecraft(cls)) {
			throw new InvalidClassException(cls);
		}
		return Type.getInternalName(cls);
	}

	default String getBaseAbstractedName(Class<?> cls) {
		String desc = Type.getInternalName(cls);
		int last = desc.lastIndexOf('/')+1;
		return desc.substring(0, last) + "Base" + desc.substring(last);
	}

	default String getAbstractedDescriptor(Class<?> cls) {
		if(this.isAbstracted(cls)) {
			String desc = Type.getDescriptor(cls);
			int last = desc.lastIndexOf('/')+1;
			return desc.substring(0, last) + "I" + desc.substring(last);
		} else if(this.isMinecraft(cls)) {
			throw new InvalidClassException(cls);
		}
		return Type.getDescriptor(cls);
	}

	default boolean containsInvalidClasses(String signature) {
		if(signature == null) return false;
		try {
			SignatureRemapper remapper = new SignatureRemapper(new SignatureVisitor(ASM9) {}, this.getRemapper());
			SignatureReader reader = new SignatureReader(signature);
			reader.accept(remapper);
			return false;
		} catch (InvalidClassException e) {
			return true;
		}
	}
}
