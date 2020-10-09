package io.github.f2bb.utils.view;

import static org.objectweb.asm.Opcodes.ASM9;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import io.github.f2bb.util.AsmUtil;
import io.github.f2bb.utils.types.SignatureWriter;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

public class Views {
	private static final Logger LOGGER = Logger.getLogger("Reifier");
	static String reify(ClassView view, String signature) {
		Map<String, String> map = view.typeParameterMap;
		SignatureWriter writer = new SignatureWriter() {
			private final Set<String> protectedScope = new HashSet<>();

			@Override
			public void visitFormalTypeParameter(String name) {
				super.visitFormalTypeParameter(name);
				this.protectedScope.add(name);
			}

			@Override
			public void visitTypeVariable(String name) {
				if (!this.protectedScope.contains(name)) {
					String str = map.get(name);
					if (str == null) {
						LOGGER.warning("Type Variable '" + name + "' did not have mapping");
					} else {
						this.stringBuilder.append(str);
						return;
					}
				}
				super.visitFormalTypeParameter(name);
			}
		};

		SignatureReader reader = new SignatureReader(signature);
		reader.accept(writer);
		String sign = writer.toString();
		return view.viewer.map(c -> reify(c, sign)).orElse(sign);
	}

	/**
	 * find the method descriptor give the class signature and the method signature
	 */
	static String getDesc(String classSign, String methodSign) {
		RawFinder newFinder = new RawFinder();
		if(classSign != null) {
			RawFinder visitor = new RawFinder();
			SignatureReader reader = new SignatureReader(classSign);
			reader.accept(visitor);
			newFinder.rawBounds = visitor.rawBounds;
		}

		SignatureReader reader = new SignatureReader(methodSign);
		reader.accept(newFinder);
		return newFinder.toString();
	}

	static ClassView getRoot(ClassView view) {
		return view.viewer.map(Views::getRoot).orElse(view);
	}

	private static final SignatureVisitor VOID_VISITOR = new SignatureVisitor(ASM9) {};
	private static class RawFinder extends org.objectweb.asm.signature.SignatureWriter {

		private String name;
		protected Map<String, String> rawBounds = new HashMap<>();

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
}
