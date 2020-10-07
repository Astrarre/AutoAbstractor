package io.github.f2bb.abstraction.inter;

import io.github.f2bb.classpath.AbstractorClassLoader;

public class ImplAsmInterfaceAbstracter extends ApiAsmInterfaceAbstracter {
	public ImplAsmInterfaceAbstracter(AbstractorClassLoader loader, Class<?> cls) {
		super(loader, cls);
	}
}
