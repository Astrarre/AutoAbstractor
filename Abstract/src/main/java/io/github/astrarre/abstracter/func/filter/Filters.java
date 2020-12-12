package io.github.astrarre.abstracter.func.filter;

import java.lang.reflect.Type;
import java.util.function.Predicate;

import io.github.astrarre.abstracter.ex.InvalidClassException;
import io.github.astrarre.abstracter.util.reflect.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.SignatureRemapper;
import org.objectweb.asm.signature.SignatureVisitor;

public interface Filters extends Opcodes {
	SignatureVisitor EMPTY_VISITOR = new SignatureVisitor(ASM9) {};
	Predicate<Type> IS_VALID = t -> {
		SignatureRemapper remapper = new SignatureRemapper(EMPTY_VISITOR, TypeUtil.REMAPPER);
		try {
			TypeUtil.visit(remapper, t, false);
			return true;
		} catch (InvalidClassException e) {
			return false;
		}
	};
}