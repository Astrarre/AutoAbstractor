package io.github.astrarre.abstracter.func.filter;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.function.BiPredicate;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.logging.Logger;

import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.func.map.TypeMappingFunction;
import org.objectweb.asm.Opcodes;

@SuppressWarnings ("unchecked")
public interface MemberFilter<T extends Member> extends Opcodes {
	Logger LOGGER = Logger.getLogger("Abstract");
	MemberFilter<Member> PUBLIC = withAccess(Modifier::isPublic);
	MemberFilter<Member> PROTECTED = withAccess(Modifier::isProtected);
	MemberFilter<Member> ABSTRACT = withAccess(Modifier::isAbstract);
	MemberFilter<Member> STATIC = withAccess(Modifier::isStatic);
	MemberFilter<Member> SYNTHETIC = withAccess(i -> (i & ACC_SYNTHETIC) != 0);
	MemberFilter<Member> BRIDGE = withAccess(i -> (i & ACC_BRIDGE) != 0);
	MemberFilter<Member> FINAL = withAccess(Modifier::isFinal);
	MemberFilter<Member> USER_DECLARED = SYNTHETIC.or(BRIDGE).negate();

	MemberFilter<Member> VISIBLE = PUBLIC.or(PROTECTED).and(USER_DECLARED);
	MemberFilter<Member> ACCESSIBLE = PUBLIC.and(USER_DECLARED);


	MemberFilter<Executable> VALID_PARAMETERS = withParameters(Filters.IS_VALID).and(withFormals(Filters.IS_VALID));

	MemberFilter<Method> VALID_PARAMS_VARS_AND_RETURN = MemberFilter.<Method>withParameters(Filters.IS_VALID)
			                                                    .and(withReturn(Filters.IS_VALID))
			                                                    .and(withFormals(Filters.IS_VALID));


	MemberFilter<Field> VALID_TYPE = MemberFilter.withType(Filters.IS_VALID);

	/**
	 * check if the method is valid
	 */
	boolean test(AbstracterConfig config, Class<?> abstracting, T method);

	static <T extends Member> MemberFilter<T> userDeclared() {
		return (MemberFilter<T>) USER_DECLARED;
	}

	static MemberFilter<Method> withName(Predicate<String> name) {
		return (config, c, m) -> name.test(m.getName());
	}

	static MemberFilter<Field> withType(BiPredicate<AbstracterConfig, Type> typePredicate) {
		return (config, c, f) -> typePredicate.test(config, TypeMappingFunction.reify(c, f.getGenericType()));
	}

	static <T extends Member> MemberFilter<T> withAccess(IntPredicate access) {
		return (config, c, m) -> access.test(m.getModifiers());
	}

	static <T extends Executable> MemberFilter<T> withFormals(BiPredicate<AbstracterConfig, Type> formalTypePredicate) {
		return (config, c, m) -> {
			TypeMappingFunction function = TypeMappingFunction.reify(c);
			for (TypeVariable<?> parameter : m.getTypeParameters()) {
				for (Type bound : parameter.getBounds()) {
					if (!formalTypePredicate.test(config, function.map(bound))) {
						return false;
					}
				}
			}
			return true;
		};
	}

	static <T extends Executable> MemberFilter<T> withParameters(BiPredicate<AbstracterConfig, Type> typePredicate) {
		return (config, c, m) -> {
			TypeMappingFunction function = TypeMappingFunction.reify(c);
			for (Type type : m.getGenericParameterTypes()) {
				if (!typePredicate.test(config, function.map(type))) {
					return false;
				}
			}
			return true;
		};
	}

	static MemberFilter<Method> withReturn(BiPredicate<AbstracterConfig, Type> typePredicate) {
		return (config, c, m) -> typePredicate.test(config, TypeMappingFunction.reify(c, m.getGenericReturnType()));
	}

	default MemberFilter<T> or(MemberFilter<T> filter) {
		return (config, c, m) -> this.test(config, c, m) || filter.test(config, c, m);
	}

	default MemberFilter<T> and(MemberFilter<T> filter) {
		return (config, c, m) -> this.test(config, c, m) && filter.test(config, c, m);
	}

	default MemberFilter<T> negate() {
		return (config, c, m) -> !this.test(config, c, m);
	}
}
