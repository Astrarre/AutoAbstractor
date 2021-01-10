package io.github.astrarre.abstracter.func.filter;

import java.lang.reflect.Type;
import java.util.function.BiPredicate;

import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.ex.InvalidClassException;
import io.github.astrarre.abstracter.util.AsmUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureVisitor;

public interface Filters extends Opcodes {
	SignatureVisitor EMPTY_VISITOR = new SignatureVisitor(ASM9) {};
	BiPredicate<AbstracterConfig, Type> IS_VALID = (config, t) -> {
		try {
			AsmUtil.visit(config, EMPTY_VISITOR, t);
			return true;
		} catch (InvalidClassException e) {
			return false;
		}
	};
}
