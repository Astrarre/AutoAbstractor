package io.github.astrarre.abstracter.abs.method;

import java.lang.reflect.Constructor;
import java.lang.reflect.TypeVariable;
import java.util.function.Consumer;

import com.google.common.reflect.TypeToken;
import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class InterfaceConstructorAbstracter extends MethodAbstracter<Constructor<?>> {
	private final String internalName = Type.getInternalName(this.method.getDeclaringClass());
	private final TypeToken<?> ret = TypeToken.of(this.method.getDeclaringClass());
	public InterfaceConstructorAbstracter(AbstractAbstracter abstracter, Constructor<?> method, boolean impl) {
		super(abstracter, method, impl);
	}

	@Override
	public String methodDescriptor(TypeToken<?>[] parameters, TypeToken<?> returnType) {
		return super.methodDescriptor(parameters, this.ret);
	}

	@Override
	public String methodSignature(TypeVariable<?>[] variables, TypeToken<?>[] parameters, TypeToken<?> returnType) {
		return super.methodSignature(variables, parameters, this.ret);
	}

	@Override
	public Header getHeader() {
		Header header = super.getHeader();
		header.name = "newInstance";
		header.access |= ACC_STATIC;
		return header;
	}

	@Override
	protected void invokeTarget(MethodNode node) {
		this.invoke(node, this.internalName,
				"<init>",
				Type.getConstructorDescriptor(this.method),
				this.getOpcode(this.method, INVOKESPECIAL));
	}

	@Override
	protected int loadThis(MethodNode node) {
		node.visitTypeInsn(NEW, this.internalName);
		node.visitInsn(DUP);
		return 0;
	}

	@Override
	public void cast(AbstractAbstracter.Location location, Type fromType, Type toType, MethodNode visitor, Consumer<MethodVisitor> apply) {
		if(location == AbstractAbstracter.Location.RETURN && fromType.getSort() == Type.VOID && toType.getSort() == Type.OBJECT) {
			apply.accept(visitor);
			return;
		}
		super.cast(location, fromType, toType, visitor, apply);
	}
}
