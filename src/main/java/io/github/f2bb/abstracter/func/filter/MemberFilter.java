package io.github.f2bb.abstracter.func.filter;

import static io.github.f2bb.abstracter.func.filter.Filters.IS_ABSTRACTED;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.logging.Logger;

import io.github.f2bb.abstracter.Abstracter;
import io.github.f2bb.abstracter.func.map.TypeMappingFunction;

@SuppressWarnings ("unchecked")
public interface MemberFilter<T extends Member> {
	Logger LOGGER = Logger.getLogger("Abstract");
	MemberFilter<?> USER_DECLARED = withAccess(Filters.SYNTHETIC).or(withAccess(Filters.BRIDGE)).negate();
	MemberFilter<Executable> VALID_PARAMETERS = withParameters(IS_ABSTRACTED);
	MemberFilter<Method> VALID_PARAMS_AND_RETURN = (MemberFilter.<Method>withParameters(IS_ABSTRACTED).and(withReturn(
			IS_ABSTRACTED))).or(MemberFilter.<Method>withAccess(Filters.ABSTRACT).and((c, m) -> {
		LOGGER.severe("Method " + m + " could not be abstracted because it contained a non" + "-abstracted class, but " +
		              "it is `abstract`, if the method is not exposed, it will" + " cause an abstract exception " +
		              "thing");
		return false;
	}));
	MemberFilter<Field> MINECRAFT_TYPE = (c, f) -> Abstracter.isMinecraft(TypeMappingFunction.raw(c, f.getGenericType()));

	static <T extends Member> MemberFilter<T> userDeclared() {
		return (MemberFilter<T>) USER_DECLARED;
	}

	boolean test(Class<?> abstracting, T method);

	default MemberFilter<T> or(MemberFilter<T> filter) {
		return (c, m) -> this.test(c, m) || filter.test(c, m);
	}

	default MemberFilter<T> and(MemberFilter<T> filter) {
		return (c, m) -> this.test(c, m) && filter.test(c, m);
	}

	default MemberFilter<T> negate() {
		return (c, m) -> !this.test(c, m);
	}

	static MemberFilter<Method> withName(Predicate<String> name) {
		return (c, m) -> name.test(m.getName());
	}

	static MemberFilter<Field> withType(Predicate<Type> typePredicate) {
		return (c, f) -> typePredicate.test(TypeMappingFunction.reify(c, f.getGenericType()));
	}

	static <T extends Member> MemberFilter<T> withAccess(IntPredicate access) {
		return (c, m) -> access.test(m.getModifiers());
	}

	static <T extends Executable> MemberFilter<T> withParameters(Predicate<Type> typePredicate) {
		return (c, m) -> {
			TypeMappingFunction function = TypeMappingFunction.reify(c);
			for (Type type : m.getGenericParameterTypes()) {
				if (!typePredicate.test(function.map(type))) {
					return false;
				}
			}
			return true;
		};
	}

	static MemberFilter<Method> withReturn(Predicate<Type> typePredicate) {
		return (c, m) -> typePredicate.test(TypeMappingFunction.reify(c, m.getGenericReturnType()));
	}
}
