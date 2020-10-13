package io.github.f2bb.abstracter.func.filter;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import io.github.f2bb.abstracter.Abstracter;
import io.github.f2bb.abstracter.ex.InvalidClassException;
import io.github.f2bb.abstracter.impl.AsmAbstracter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.SignatureRemapper;

public interface Filters extends Opcodes {
	Predicate<Type> IS_ABSTRACTED = t -> {
		SignatureRemapper remapper = new SignatureRemapper(Abstracter.EMPTY_VISITOR, Abstracter.REMAPPER);
		try {
			AsmAbstracter.visit(remapper, t);
			return true;
		} catch (InvalidClassException e) {
			return false;
		}
	};

	Predicate<Class<?>> IS_CLASS_ABSTRACTED = Abstracter::isAbstracted;

	IntPredicate PROTECTED = Modifier::isProtected;
	IntPredicate ABSTRACT = Modifier::isAbstract;
	IntPredicate STATIC = Modifier::isStatic;
	IntPredicate PUBLIC = Modifier::isPublic;
	IntPredicate SYNTHETIC = i -> (i & ACC_SYNTHETIC) != 0;
	IntPredicate BRIDGE = i -> (i & ACC_BRIDGE) != 0;
	IntPredicate FINAL = Modifier::isFinal;
}
