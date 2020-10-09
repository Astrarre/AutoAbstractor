package io.github.f2bb.utils.types;

import static org.objectweb.asm.Opcodes.ASM9;

import java.util.HashMap;
import java.util.Map;

import io.github.f2bb.util.AsmUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

public class RawFinder extends SignatureWriter {
	public static final SignatureVisitor VOID_VISITOR = new SignatureVisitor(ASM9) {};
	private String name;
	protected Map<String, String> rawBounds = new HashMap<>();

	/**
	 * gets the raw bounds for a class
	 */
	private static RawFinder rawBounds(String sign) {
		RawFinder visitor = new RawFinder();
		SignatureReader reader = new SignatureReader(sign);
		reader.accept(visitor);
		return visitor;
	}

	/**
	 * find the descriptor give the class signature and the method signature
	 */
	public static String getDesc(String classSign, String methodSign) {
		RawFinder visitor = rawBounds(classSign);
		RawFinder newFinder = new RawFinder();
		newFinder.rawBounds = visitor.rawBounds;
		SignatureReader reader = new SignatureReader(methodSign);
		reader.accept(newFinder);
		return newFinder.toString();
	}

	@Override
	public void visitFormalTypeParameter(String name) {
		this.name = name;
	}

	@Override
	public SignatureVisitor visitClassBound() {
		String name = this.name;
		// prevent interface bounds from mucking stuff up
		this.name = null;
		return new RawFinder() {
			@Override
			public void visitEnd() {
				super.visitEnd();
				RawFinder.this.rawBounds.put(name, this.toString());
			}
		};
	}

	@Override
	public SignatureVisitor visitInterfaceBound() {
		if(this.name != null) {
			String name = this.name;
			this.name = null;
			return new RawFinder() {
				@Override
				public void visitEnd() {
					super.visitEnd();
					RawFinder.this.rawBounds.put(name, this.toString());
				}
			};
		} else return VOID_VISITOR;
	}

	@Override
	public void visitTypeVariable(String name) {
		super.visitClassType(this.rawBounds.getOrDefault(name, AsmUtil.OBJECT_DESC).substring(1));
	}

	@Override
	public SignatureVisitor visitTypeArgument(char wildcard) {
		return VOID_VISITOR;
	}

	@Override
	public SignatureVisitor visitExceptionType() {
		return VOID_VISITOR;
	}

	@Override
	public String toString() {
		return super.toString().replace('.', '$');
	}
}
