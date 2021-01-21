package io.github.astrarre.abstracter.abs.method;

import static io.github.astrarre.abstracter.util.AsmUtil.map;

import java.lang.reflect.Constructor;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.reflect.TypeToken;
import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import io.github.astrarre.abstracter.func.map.TypeMappingFunction;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class InterfaceConstructorAbstracter extends MethodAbstracter<Constructor<?>> {
	private final String internalName = Type.getInternalName(this.member.getDeclaringClass());

	public InterfaceConstructorAbstracter(AbstracterConfig config, AbstractAbstracter abstracter, Constructor<?> method, boolean impl) {
		super(config, abstracter, method, impl);
	}

	@Override
	public Header getHeader() {
		Function<java.lang.reflect.Type, TypeToken<?>> resolve = TypeMappingFunction.resolve(this.abstracter.getCls(this.config));
		TypeToken<?>[] params = map(this.member.getGenericParameterTypes(), resolve, TypeToken[]::new);
		TypeToken<?> returnType = resolve.apply(this.member.getDeclaringClass());
		String desc = this.methodSignature(EMPTY, params, returnType, TypeToken::getRawType);
		String sign = this.impl ? null : this.methodSignature(this.member.getTypeParameters(), params, returnType, TypeToken::getType);
		if (desc.equals(sign)) {
			sign = null;
		}
		int access = this.member.getModifiers();
		return new Header(access | ACC_STATIC, "newInstance", desc, sign);
	}

	@Override
	protected void invokeTarget(MethodNode node) {
		this.invoke(node, this.internalName, "<init>", Type.getConstructorDescriptor(this.member), this.getOpcode(this.member, INVOKESPECIAL));
	}

	@Override
	protected int loadThis(MethodNode node) {
		node.visitTypeInsn(NEW, this.internalName);
		node.visitInsn(DUP);
		return 0;
	}

	@Override
	public void cast(AbstractAbstracter.Location location, Type fromType, Type toType, MethodNode visitor, Consumer<MethodVisitor> apply) {
		if (location == AbstractAbstracter.Location.RETURN && fromType.getSort() == Type.VOID && toType.getSort() == Type.OBJECT) {
			apply.accept(visitor);
			return;
		}
		super.cast(location, fromType, toType, visitor, apply);
	}
}
