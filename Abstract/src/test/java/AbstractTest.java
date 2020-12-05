import static io.github.astrarre.abstracter.AbstracterUtil.registerConstantlessInterface;
import static io.github.astrarre.abstracter.AbstracterUtil.registerDefaultBase;
import static io.github.astrarre.abstracter.AbstracterUtil.registerDefaultConstants;
import static io.github.astrarre.abstracter.AbstracterUtil.registerDefaultInterface;
import static io.github.astrarre.abstracter.AbstracterConfig.registerConstants;
import static io.github.astrarre.abstracter.AbstracterConfig.registerInnerOverride;
import static io.github.astrarre.abstracter.AbstracterConfig.registerInterface;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.reflect.TypeToken;
import io.github.astrarre.Access;
import io.github.astrarre.abstracter.AbstracterUtil;
import io.github.astrarre.abstracter.abs.ConstantsAbstracter;
import io.github.astrarre.abstracter.abs.InterfaceAbstracter;
import io.github.astrarre.abstracter.util.AbstracterLoader;
import v0.io.github.astrarre.block.Materials;

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
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
	public static void main(String[] args) throws IOException {
		// todo wait for player's TR patch to go on maven
		List<File> classpath = Arrays.asList(new File("classpath").listFiles());
		for (File file : classpath) {
			AbstracterLoader.CLASSPATH.addURL(file.toURI().toURL());
		}
		AbstracterLoader.INSTANCE.addURL(new File("fodder.jar").toURI().toURL());
		// settings
		registerInterface(AbstractBlock.Settings.class,
				c -> new InterfaceAbstracter(c, "v0/io/github/astrarre/block/IBlock$Settings").extension(AbstractTest::test)
				                                                                          .attach(new TypeToken<Consumer<String>>() {}));

		registerInnerOverride(Block.class, AbstractBlock.Settings.class);

		// attachment interfaces > extension methods, cus no javadoc
		registerDefaultConstants(Blocks.class, Items.class);
		registerConstants(Material.class, c -> new ConstantsAbstracter(c, "v0/io/github/astrarre/block/Materials"));

		registerConstantlessInterface(Material.class);

		registerDefaultInterface(Block.class,
				ItemStack.class,
				Item.class,
				Item.Settings.class,
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
				EntityPose.class);
		// base
		registerDefaultBase(Block.class, Entity.class, Enchantment.class, Item.class, Material.class);

		AbstracterUtil
				.apply(classpath, "api.jar", "api_sources.jar", "impl.jar", "manifest.properties", "mappings.tiny");
	}

	@Access (Modifier.STATIC | Modifier.PUBLIC)
	public static void test(Object _this) {}

}
