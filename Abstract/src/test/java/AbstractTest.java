import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

import io.github.f2bb.abstracter.Abstracter;
import io.github.f2bb.abstracter.AbstracterConfig;
import io.github.f2bb.abstracter.func.string.ToStringFunction;
import io.github.f2bb.abstracter.util.AbstracterLoader;

public class AbstractTest {
	public static void main(String[] args) throws IOException {
		for (File file : new File("classpath").listFiles()) {
			AbstracterLoader.CLASSPATH.addURL(file.toURI().toURL());
		}
		AbstracterLoader.INSTANCE.addURL(new File("fodder.jar").toURI().toURL());
		// settings
		ToStringFunction<Class<?>> abstractSettings = ToStringFunction.constant("io/github/f2bb/block/IBlock$Settings");
		AbstracterConfig.registerInterface("net.minecraft.block.AbstractBlock$Settings",
				Abstracter.INTERFACE_API_ASM.asBuilder().nameFunction(abstractSettings).build(),
				Abstracter.INTERFACE_IMPL_ASM.asBuilder().nameFunction(abstractSettings).build(),
				Abstracter.INTERFACE_API_JAVA.asBuilder().nameFunction(abstractSettings).build());

		AbstracterConfig.overrideInnerClass("net.minecraft.block.Block", "net.minecraft.block.AbstractBlock$Settings");
		registerDefaultInterface("net.minecraft.block.Block");
		registerDefaultInterface("net.minecraft.item.Item");
		registerDefaultInterface("net.minecraft.item.Item$Settings");
		registerDefaultInterface("net.minecraft.block.Blocks");
		registerDefaultInterface("net.minecraft.block.BlockState");
		registerDefaultInterface("net.minecraft.block.Material");
		registerDefaultInterface("net.minecraft.util.math.BlockPos");
		registerDefaultInterface("net.minecraft.world.World");
		registerDefaultInterface("net.minecraft.world.WorldAccess");
		registerDefaultInterface("net.minecraft.entity.Entity");
		registerDefaultInterface("net.minecraft.enchantment.Enchantment");
		registerDefaultInterface("net.minecraft.Bootstrap");
		registerDefaultBase("net.minecraft.block.Block");
		registerDefaultBase("net.minecraft.entity.Entity");
		registerDefaultBase("net.minecraft.enchantment.Enchantment");
		registerDefaultBase("net.minecraft.item.Item");

		ZipOutputStream api = new ZipOutputStream(new FileOutputStream("api.jar"));
		AbstracterConfig.writeApiJar(api);
		api.close();

		ZipOutputStream impl = new ZipOutputStream(new FileOutputStream("impl.jar"));
		AbstracterConfig.writeImplJar(impl);
		impl.close();

		ZipOutputStream sources = new ZipOutputStream(new FileOutputStream("sources.jar"));
		AbstracterConfig.writeSources(sources);
		sources.close();

		FileOutputStream manifest = new FileOutputStream("manifest.properties");
		AbstracterConfig.writeManifest(manifest);
		manifest.close();
	}

	private static void registerDefaultInterface(String cls) {
		AbstracterConfig.registerInterface(cls,
				Abstracter.INTERFACE_API_ASM,
				Abstracter.INTERFACE_IMPL_ASM,
				Abstracter.INTERFACE_API_JAVA);
	}

	private static void registerDefaultBase(String base) {
		AbstracterConfig
				.registerBase(base, Abstracter.BASE_API_ASM, Abstracter.BASE_IMPL_ASM, Abstracter.BASE_API_JAVA);
	}
}
