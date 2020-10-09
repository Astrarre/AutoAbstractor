package net.devtech.test.signatures;

import io.github.f2bb.utils.types.RawFinder;

public class B {
	public static void main(String[] args) throws ClassNotFoundException {
		System.out.println(RawFinder.getDesc("<P:Lnet/minecraft/world/gen/tree/TreeDecorator;>Ljava/lang/Object;",
				"<P:Lnet/minecraft/world/gen/tree/TreeDecorator;>(TP;" +
				"Lcom/mojang/serialization/Codec<TP;>;)Lnet/minecraft/world/gen/tree/TreeDecoratorType<TP;>;"));
	}
}
