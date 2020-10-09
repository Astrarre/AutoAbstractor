package io.github.f2bb.utils.view;

import io.github.f2bb.Abstracter;
import org.objectweb.asm.tree.MethodNode;

public class MethodView {
	public final ClassView viewer;
	public final MethodNode node;
	public final String reifiedSignature;
	public final String descriptor;
	public final boolean shouldAbstract;

	public MethodView(Abstracter abstracter, ClassView viewer, MethodNode node) {
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

	@Override
	public String toString() {
		return this.viewer.node.name + ";" + this.node.name + ";" + this.node.desc;
	}


}