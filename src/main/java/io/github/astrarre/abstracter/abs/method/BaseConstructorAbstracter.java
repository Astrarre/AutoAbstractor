package io.github.astrarre.abstracter.abs.method;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

// todo write bridges as intermediary
// todo then remove super class
// todo then add the super class at runtime
// todo this way TR does not accidentally remap the bridge and crash with duplicates
public class BaseConstructorAbstracter extends MethodAbstracter<Constructor<?>> {
	public BaseConstructorAbstracter(AbstracterConfig config, AbstractAbstracter abstracter, Constructor<?> method, boolean impl) {
		super(config, abstracter, method, impl);
	}


	@Override
	public Header getHeader() {
		Header header = super.getHeader();
		header.name = "<init>";
		return header;
	}
	@Override
	protected void invokeTarget(MethodNode node) {
		this.invoke(node,
				Type.getInternalName(this.member.getDeclaringClass()),
				"<init>",
				Type.getConstructorDescriptor(this.member),
				this.getOpcode(this.member, INVOKESPECIAL));
	}
}
