package io.github.f2bb.classpath;

import io.github.f2bb.invalid.InvalidTypeMappingException;
import io.github.f2bb.util.AsmUtil;
import io.github.f2bb.util.Util;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.signature.SignatureVisitor;

public class AbstractorRemapper extends Remapper {
	private final AbstractorClassLoader classLoader;

	public AbstractorRemapper(AbstractorClassLoader classLoader) {this.classLoader = classLoader;}

	@Override
	public String mapType(String internalName) {
		try {
			Class<?> cls = this.classLoader.loadClass(internalName.replace('/', '.'));
			if (this.classLoader.isMinecraft(cls)) {
				if (this.classLoader.canAbstractClass(cls)) {
					return AsmUtil.prefixName("I", internalName);
				} else {
					throw new InvalidTypeMappingException(internalName);
				}
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		return internalName;
	}

	@Override
	public String[] mapTypes(String[] internalNames) {
		return Util.map(internalNames, this::mapType, String[]::new);
	}

	// expose class
	@Override
	public SignatureVisitor createSignatureRemapper(SignatureVisitor signatureVisitor) {
		return super.createSignatureRemapper(signatureVisitor);
	}
}
