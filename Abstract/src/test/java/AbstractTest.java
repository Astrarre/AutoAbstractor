import static io.github.f2bb.abstracter.AbstracterConfig.registerInnerOverride;
import static io.github.f2bb.abstracter.AbstracterConfig.registerInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipOutputStream;

import io.github.f2bb.abstracter.AbstracterConfig;
import io.github.f2bb.abstracter.abs.BaseAbstracter;
import io.github.f2bb.abstracter.abs.InterfaceAbstracter;
import io.github.f2bb.abstracter.util.AbstracterLoader;
import io.github.f2bb.decompiler.Decompile;

import net.minecraft.Bootstrap;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

@SuppressWarnings ("ConstantConditions")
public class AbstractTest {
	public static void main(String[] args) throws IOException {
		List<File> classpath = Arrays.asList(new File("classpath").listFiles());
		for (File file : classpath) {
			AbstracterLoader.CLASSPATH.addURL(file.toURI().toURL());
		}
		AbstracterLoader.INSTANCE.addURL(new File("fodder.jar").toURI().toURL());
		// settings
		registerInterface(AbstractBlock.Settings.class,
				c -> new InterfaceAbstracter(c, "v0/io/github/f2bb/block/IBlock$Settings"));
		registerInnerOverride(Block.class, AbstractBlock.Settings.class);

		// attachment interfaces > extension methods, cus no javadoc
		registerDefaultInterface(Block.class);
		registerDefaultInterface(Item.class);
		registerDefaultInterface(Item.Settings.class);
		registerDefaultInterface(Blocks.class);
		registerDefaultInterface(BlockState.class);
		registerDefaultInterface(Material.class);
		registerDefaultInterface(BlockPos.class);
		registerDefaultInterface(World.class);
		registerDefaultInterface(WorldAccess.class);
		registerDefaultInterface(Entity.class);
		registerDefaultInterface(Enchantment.class);
		registerDefaultInterface(Bootstrap.class);

		// base
		registerDefaultBase(Block.class);
		registerDefaultBase(Entity.class);
		registerDefaultBase(Enchantment.class);
		registerDefaultBase(Item.class);

		ZipOutputStream api = new ZipOutputStream(new FileOutputStream("api.jar"));
		AbstracterConfig.writeJar(api, false);
		api.close();

		ZipOutputStream impl = new ZipOutputStream(new FileOutputStream("impl.jar"));
		AbstracterConfig.writeJar(impl, true);
		impl.close();

		FileOutputStream manifest = new FileOutputStream("manifest.properties");
		AbstracterConfig.writeManifest(manifest);
		manifest.close();

		Decompile.decompile(classpath,
				new File("api.jar"),
				new File("api_sources.jar"),
				new File("lines.lmap"),
				new File("mappings.tiny"));
	}

	private static void registerDefaultInterface(Class<?> cls) {
		registerInterface(cls, InterfaceAbstracter::new);
	}

	private static void registerDefaultBase(Class<?> base) {
		AbstracterConfig.registerBase(base, BaseAbstracter::new);
	}
}
