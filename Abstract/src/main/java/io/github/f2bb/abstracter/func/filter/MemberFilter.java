package io.github.f2bb.abstracter.func.filter;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.logging.Logger;

import io.github.f2bb.abstracter.func.map.TypeMappingFunction;
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


	MemberFilter<Executable> VALID_PARAMETERS = withParameters(Filters.IS_VALID);
	
	MemberFilter<Method> VALID_PARAMS_AND_RETURN = MemberFilter.<Method>withParameters(Filters.IS_VALID)
			                                               .and(withReturn(Filters.IS_VALID));

	MemberFilter<Field> VALID_TYPE = MemberFilter.withType(Filters.IS_VALID);
	
	static <T extends Member> MemberFilter<T> userDeclared() {
		return (MemberFilter<T>) USER_DECLARED;
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

	default MemberFilter<T> or(MemberFilter<T> filter) {
		return (c, m) -> this.test(c, m) || filter.test(c, m);
	}

	boolean test(Class<?> abstracting, T method);

	default MemberFilter<T> and(MemberFilter<T> filter) {
		return (c, m) -> this.test(c, m) && filter.test(c, m);
	}

	default MemberFilter<T> negate() {
		return (c, m) -> !this.test(c, m);
	}
}
