package pkg.test;

import v0.io.github.f2bb.IBootstrap;
import v0.io.github.f2bb.block.BaseBlock;
import v0.io.github.f2bb.block.IBlock;
import v0.io.github.f2bb.block.IBlockState;
import v0.io.github.f2bb.block.IMaterial;
import v0.io.github.f2bb.block.Materials;
import v0.io.github.f2bb.entity.IEntityPose;
import v0.io.github.f2bb.util.math.IBlockPos;
import v0.io.github.f2bb.world.IWorldAccess;

public class Test {
	public static void main() {
		try {
			System.out.println(IEntityPose.CROUCHING);
			IBootstrap.initialize();
			IMaterial material = Materials.AIR;
			IBlock block = new BaseBlock(IBlock.Settings.of(material)) {
				@Override
				public void onBroken(IWorldAccess arg0, IBlockPos arg1, IBlockState arg2) {
					arg0.updateNeighbors(arg1, arg2.getBlock());
				}
			};
			IBlock b = IBlock.newInstance(IBlock.Settings.of(material));
			System.out.println(block.getBlastResistance() + " = " + b.getBlastResistance());
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
