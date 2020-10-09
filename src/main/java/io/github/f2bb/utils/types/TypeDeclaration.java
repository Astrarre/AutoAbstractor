package io.github.f2bb.utils.types;

import static org.objectweb.asm.Opcodes.ASM9;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.f2bb.utils.Abstracter;
import io.github.f2bb.util.AsmUtil;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.ClassNode;

public class TypeDeclaration {
	public static Map<String, String> createArgumentMapping(Arguments args, Abstracter abstracter, String cls) {
		return createArgumentMapping(getTypeDeclarations(abstracter, cls), args);
	}

	public static Map<String, String> createArgumentMapping(List<List<String>> declarations, Arguments args) {
		Map<String, String> map = new HashMap<>();
		for (int inner = 0; inner < declarations.size(); inner++) {
			List<String> in = declarations.get(inner);
			for (int index = 0; index < in.size(); index++) {
				String identifier = in.get(index);
				map.put(identifier, args.getSafeish(inner, index));
			}
		}
		return map;
	}

	public static List<List<String>> getTypeDeclarations(Abstracter abstracter, String cls) {
		List<List<String>> declarations = new ArrayList<>();
		for (String s : AsmUtil.splitName(cls)) {
			ClassNode node = abstracter.getClass(s);
			declarations.add(0, getFormalTypeDeclarations(node.signature));
		}
		return declarations;
	}

	private static List<String> getFormalTypeDeclarations(String signature) {
		List<String> declarations = new ArrayList<>();
		SignatureVisitor visitor = new SignatureVisitor(ASM9) {
			@Override
			public void visitFormalTypeParameter(String name) {
				declarations.add(name);
			}
		};
		SignatureReader reader = new SignatureReader(signature);
		reader.accept(visitor);
		return declarations;
	}
}
