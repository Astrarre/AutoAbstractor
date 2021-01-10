package io;

import static io.github.astrarre.abstracter.AbstracterConfig.registerInterface;
import static io.github.astrarre.abstracter.AbstracterUtil.registerDefaultBase;
import static io.github.astrarre.abstracter.AbstracterUtil.registerDefaultInterface;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Function;

import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.AbstracterUtil;
import io.github.astrarre.abstracter.Access;
import io.github.astrarre.abstracter.abs.InterfaceAbstracter;

import net.minecraft.Bootstrap;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.block.MaterialColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

@SuppressWarnings ({
		"ConstantConditions",
		"UnstableApiUsage"
})
public class AbstractTest {

	public static void main(String[] args) throws IOException, InterruptedException {
		// todo wait for player's TR patch to go on maven

		Files.newBufferedReader(Paths.get("classpath.txt"))
		     .lines()
		     .map(File::new)
		     .map(File::toURI)
		     .map((TFunction<URI, URL>) URI::toURL)
		     .forEach(AbstracterConfig.CLASSPATH::addURL);
		AbstracterConfig.INSTANCE.addURL(new File("fodder.jar").toURI().toURL());
		// settings

		//registerInnerOverride(Block.class, AbstractBlock.Settings.class);

		// attachment interfaces > extension methods, cus no javadoc
		AbstracterConfig.registerInterface(new InterfaceAbstracter(Material.class, "io/github/astrarre/v0/block/Materials"));

		registerInterface(new InterfaceAbstracter(Item.class)).addInner(registerInterface(new InterfaceAbstracter(Item.Settings.class)));
		registerInterface(new InterfaceAbstracter(Block.class)).addInner(registerInterface(new InterfaceAbstracter(AbstractBlock.Settings.class,
				"io/github/astrarre/v0/block/Block$Settings")));

		registerDefaultInterface(EntityPose.class,
				EnchantmentTarget.class,
				Hand.class,
				Blocks.class,
				Items.class,
				Block.class,
				ItemStack.class,
				BlockState.class,
				BlockPos.class,
				World.class,
				WorldAccess.class,
				Entity.class,
				Enchantment.class,
				Bootstrap.class,
				StatusEffectInstance.class,
				MinecraftClient.class,
				ClientWorld.class,
				EntityType.class,
				MaterialColor.class,
				Vec3d.class,
				Vec3i.class,
				LivingEntity.class);
		// base
		registerDefaultBase(Block.class, Entity.class, Enchantment.class, Item.class, Material.class);
		AbstracterUtil.applyParallel("api.jar", "api_sources.jar", "impl.jar", "mappings.tiny");
	}

	@Access (Modifier.STATIC | Modifier.PUBLIC)
	public static void test(Object _this) {}

	private interface TFunction<A, B> extends Function<A, B> {
		@Override
		default B apply(A a) {
			try {
				return this.applyT(a);
			} catch (Throwable throwable) {
				throw new RuntimeException(throwable);
			}
		}

		B applyT(A val) throws Throwable;
	}

}
