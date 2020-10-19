package net.devtech.test;

import io.github.f2bb.IBootstrap;
import io.github.f2bb.block.IBlocks;

import net.minecraft.block.Block;

public class Testing {
	public static void main(String[] args) {
		IBootstrap.initialize();
		System.out.println(IBlocks.ACACIA_BUTTON);
		MyBlock block = new MyBlock();
		Block minecraft = (Block) (Object) block;
		minecraft.onLandedUpon(null, null, null, 0f);
	}
}
