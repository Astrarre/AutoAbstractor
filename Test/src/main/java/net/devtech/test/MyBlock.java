package net.devtech.test;

import io.github.f2bb.block.BaseBlock;
import io.github.f2bb.block.IBlock;
import io.github.f2bb.block.IMaterial;
import io.github.f2bb.entity.IEntity;
import io.github.f2bb.util.math.IBlockPos;
import io.github.f2bb.world.IWorld;

public class MyBlock extends BaseBlock {
	public MyBlock() {
		super(IBlock.Settings.of(IMaterial.AIR));
	}

	@Override
	public void onLandedUpon(IWorld arg0, IBlockPos arg1, IEntity arg2, float arg3) {
		System.out.println("HOLY SHIT IT WORKS!");
	}
}
