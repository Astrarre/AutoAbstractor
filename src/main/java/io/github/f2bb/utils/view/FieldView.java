package io.github.f2bb.utils.view;

import io.github.f2bb.Abstracter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;

public class FieldView {
	public final ClassView viewer;
	public final FieldNode node;
	public final String reifiedSignature, descriptor;
	public final boolean shouldAbstract;

	public FieldView(Abstracter abstracter, ClassView viewer, FieldNode node) {
		this.viewer = viewer;
		this.node = node;

		String sign = this.node.signature;
		String desc = this.node.desc;
		if (sign != null) {
			sign = Views.reify(viewer, sign);
			desc = Views.getDesc(Views.getRoot(viewer).node.signature, sign);
			if(sign.equals(desc)) {
				sign = null;
			}
		}

		boolean should;
		try {
			sign = abstracter.remap(sign);
			desc = abstracter.remap(desc);
			should = abstracter.shouldAbstractSign(desc, sign);
		} catch (IllegalArgumentException e) {
			should = false;
		}

		this.shouldAbstract = should;
		this.reifiedSignature = sign;
		this.descriptor = desc;
	}
}
