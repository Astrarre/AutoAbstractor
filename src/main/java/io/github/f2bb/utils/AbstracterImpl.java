package io.github.f2bb.utils;

import io.github.f2bb.Abstracter;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class AbstracterImpl implements Opcodes, Abstracter {
	@Override
	@Nullable
	public ClassNode getClass(String clsName) {
		// todo impl
		return null;
	}

	@Override
	public boolean isMinecraft(String internalName) {
		// hahayes flawless detection
		return internalName.startsWith("net.minecraft");
	}

	@Override
	public boolean isInterfaceAbstracted(String internalName) {
		return isMinecraft(internalName) && true; // todo
	}


}
