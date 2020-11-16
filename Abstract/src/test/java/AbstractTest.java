import static io.github.f2bb.AbstracterUtil.registerDefaultBase;
import static io.github.f2bb.AbstracterUtil.registerDefaultInterface;
import static io.github.f2bb.abstracter.AbstracterConfig.registerInnerOverride;
import static io.github.f2bb.abstracter.AbstracterConfig.registerInterface;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.reflect.TypeToken;
import io.github.f2bb.AbstracterUtil;
import io.github.f2bb.Access;
import io.github.f2bb.abstracter.abs.InterfaceAbstracter;
import io.github.f2bb.abstracter.util.AbstracterLoader;

import net.minecraft.Bootstrap;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
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
				c -> new InterfaceAbstracter(c, "v0/io/github/f2bb/block/IBlock$Settings")
						     .extension(AbstractTest::test)
		.attach(new TypeToken<Consumer<String>>() {}));

		registerInnerOverride(Block.class, AbstractBlock.Settings.class);

		// attachment interfaces > extension methods, cus no javadoc
		registerDefaultInterface(Block.class,
				Item.class,
				ItemStack.class,
				Item.Settings.class,
				Blocks.class,
				BlockState.class,
				Material.class,
				BlockPos.class,
				World.class,
				WorldAccess.class,
				Entity.class,
				Enchantment.class,
				Bootstrap.class,
				StatusEffectInstance.class,
				MinecraftClient.class,
				ClientWorld.class);
		// base
		registerDefaultBase(Block.class);
		registerDefaultBase(Entity.class);
		registerDefaultBase(Enchantment.class);
		registerDefaultBase(Item.class);

		AbstracterUtil
				.apply(classpath, "api.jar", "api_sources.jar", "impl.jar", "manifest.properties", "mappings.tiny");
	}

	@Access(Modifier.STATIC | Modifier.PUBLIC)
	public static void test(Object _this) {}

}
