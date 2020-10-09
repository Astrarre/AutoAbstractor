package io.github.f2bb.utils.types;

import static org.objectweb.asm.Opcodes.ASM9;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.f2bb.util.AsmUtil;
import io.github.f2bb.utils.Abstracter;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.tree.ClassNode;

public final class Arguments extends SignatureVisitor {
	public int inner;
	public final List<List<String>> arguments = new ArrayList<>();
	public String desc;

	public String getSafeish(int inner, int index) {
		List<String> strings = this.arguments.get(inner);
		if (strings.isEmpty()) {
			return AsmUtil.OBJECT_DESC;
		} else {
			return strings.get(index);
		}
	}

	public Arguments() {
		super(ASM9);
	}

	public static List<Arguments> get(String signature) {
		List<Arguments> arguments = new ArrayList<>();
		SignatureVisitor visitor = new SignatureVisitor(ASM9) {
			@Override
			public SignatureVisitor visitSuperclass() {
				Arguments args = new Arguments();
				arguments.add(args);
				return args;
			}

			@Override
			public SignatureVisitor visitInterface() {
				Arguments args = new Arguments();
				arguments.add(args);
				return args;
			}
		};

		SignatureReader reader = new SignatureReader(signature);
		reader.accept(visitor);
		return arguments;
	}

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

	// todo replace this with something from ClassView's side
	// todo for better inner class detection
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

	@Override
	public void visitClassType(String name) {
		String[] split = name.split("\\$");
		for (int i = 0; i < split.length; i++) {
			if (i == 0) {
				this.desc = 'L' + split[i];
				this.arguments.add(new ArrayList<>());
			} else {
				this.visitInnerClassType(split[i]);
			}
		}
	}

	@Override
	public void visitInnerClassType(String name) {
		this.desc += "$" + name;
		this.arguments.add(new ArrayList<>());
		this.inner++;
	}

	private int i;
	private SignatureWriter last;

	@Override
	public SignatureVisitor visitTypeArgument(char wildcard) {
		this.writeLast();
		this.i = this.inner;
		return this.last = new SignatureWriter();
	}

	@Override
	public void visitTypeArgument() {
		this.arguments.get(this.inner).add("*");
	}

	@Override
	public void visitEnd() {
		this.writeLast();
		this.desc += ';';
	}

	private void writeLast() {
		if (this.last != null) {
			Arguments.this.arguments.get(this.i).add(this.last.toString());
			this.last = null;
		}
	}
}
