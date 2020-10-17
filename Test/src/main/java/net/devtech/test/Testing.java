package net.devtech.test;

import net.minecraft.Bootstrap;
import net.minecraft.IBootstrap;
import net.minecraft.block.IBlocks;

public class Testing {
	public static void main(String[] args) {
		Bootstrap.initialize();
		System.out.println(IBlocks.ACACIA_BUTTON);
	}
}
