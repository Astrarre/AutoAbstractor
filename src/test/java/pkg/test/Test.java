package pkg.test;

import v0.io.github.f2bb.IBootstrap;
import v0.io.github.f2bb.block.BaseBlock;
import v0.io.github.f2bb.block.CMaterial;
import v0.io.github.f2bb.block.IBlock;
import v0.io.github.f2bb.block.IMaterial;

public class Test {
	public static void main() {
		try {
			IBootstrap.initialize();
			IMaterial material = CMaterial.AIR;
			IBlock block = new BaseBlock(IBlock.Settings.of(material));
			IBlock b = IBlock.newInstance(IBlock.Settings.of(material));
			System.out.println(block.getBlastResistance() + " = " + b.getBlastResistance());
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
