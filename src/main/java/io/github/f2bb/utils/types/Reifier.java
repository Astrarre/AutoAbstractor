package io.github.f2bb.utils.types;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import io.github.f2bb.utils.view.MethodView;
import org.objectweb.asm.signature.SignatureReader;

public class Reifier {
	private static final Logger LOGGER = Logger.getLogger("Reifier");
	public static String getSignature(MethodView view) {
		String sign = view.node.signature;
		if(sign != null) {
			Map<String, String> map = view.viewer.typeParameterMap;
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

			SignatureReader reader = new SignatureReader(view.node.signature);
			reader.accept(writer);
			return writer.toString();
		}
		return null;
	}
}
