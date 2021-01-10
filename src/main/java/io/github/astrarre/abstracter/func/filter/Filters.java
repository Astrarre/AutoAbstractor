package io.github.astrarre.abstracter.func.filter;

import java.lang.reflect.Type;
import java.util.function.BiPredicate;

import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.ex.InvalidClassException;
import io.github.astrarre.abstracter.util.AsmUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SignatureRemapper;
import org.objectweb.asm.signature.SignatureVisitor;

public interface Filters extends Opcodes {
	SignatureVisitor EMPTY_VISITOR = new SignatureVisitor(ASM9) {};
	BiPredicate<AbstracterConfig, Type> IS_VALID = (config, t) -> {
		SignatureRemapper remapper = new SignatureRemapper(EMPTY_VISITOR, getRemapper(config));
		try {
			AsmUtil.visit(config, remapper, t, false);
			return true;
		} catch (InvalidClassException e) {
			return false;
		}
	};

	static Remapper getRemapper(AbstracterConfig config) {
		return new Remapper() {
			@Override
			public String map(String internalName) {
				Class<?> cls = config.getClass(internalName);
				return config.getInterfaceName(cls);
			}
		};
	}
}
