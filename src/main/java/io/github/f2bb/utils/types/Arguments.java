package io.github.f2bb.utils.types;

import static org.objectweb.asm.Opcodes.ASM9;

import java.util.ArrayList;
import java.util.List;

import io.github.f2bb.util.AsmUtil;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

public final class Arguments extends SignatureVisitor {
	public int inner;
	public final List<List<String>> arguments = new ArrayList<>();
	public String desc;

	public String getSafeish(int inner, int index) {
		List<String> strings = this.arguments.get(inner);
		if (strings.isEmpty()) {
			return AsmUtil.OBJECT;
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
