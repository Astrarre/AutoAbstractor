package io.github.astrarre.abstracter;


import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipOutputStream;

import org.zeroturnaround.zip.ZipUtil;

import net.fabricmc.mappingpoet.Main;

public class AbstracterUtil {
	public static String pkg = "io/github/astrarre/v%d/";

	public static void applyParallel(AbstracterConfig config, String args, Runnable runnable) throws IOException, InterruptedException {
		Properties properties = new Properties();
		properties.load(new FileReader(args));
		String mappings = properties.getProperty("mappings"), minecraft = properties.getProperty("minecraft"), libraries = properties.getProperty(
				"libraries"), api_jar = properties.getProperty("api_jar"), api_sources_jar = properties.getProperty("api_sources_jar"), impl_jar = properties
						                                                                                                                                   .getProperty(
								                                                                                                                                   "impl_jar");

		for (String library : libraries.split(";")) {
			File file = new File(library);
			config.classpath.addURL(file.toURI().toURL());
		}

		config.minecraft.addURL(new File(minecraft).toURI().toURL());

		runnable.run();

		applyParallel(config, api_jar, api_sources_jar, impl_jar, mappings);
	}

	public static void applyParallel(AbstracterConfig config, String apiFile, String sourcesFile, String implFile, String mappingsFile) throws InterruptedException {
		ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);
		service.submit(() -> {
			try {
				System.out.println("Writing api...");
				ZipOutputStream api = new ZipOutputStream(new FileOutputStream(apiFile));
				long startWrite = System.currentTimeMillis();
				config.writeJar(service, api, false);
				api.close();
				System.out.println("Api finished in " + (System.currentTimeMillis() - startWrite) + "ms! Now decompiling...");

				startWrite = System.currentTimeMillis();

				Path dir = Files.createTempDirectory("decomp");
				Main.generate(Paths.get(mappingsFile), Paths.get(apiFile), dir, config.nameMap());
				System.out.println("Generated sources in " + (System.currentTimeMillis() - startWrite) + "ms! Now packing sources into zip...");
				startWrite = System.currentTimeMillis();
				ZipUtil.pack(dir.toFile(), Paths.get(sourcesFile).toFile());
				System.out.println("Packed zip in " + (System.currentTimeMillis() - startWrite) + "ms! Finished api export!");
				return null;
			} catch (Throwable t) {
				t.printStackTrace();
				return null;
			}
		});

		try {
			System.out.println("Writing impl...");
			ZipOutputStream impl = new ZipOutputStream(new FileOutputStream(implFile));
			long startWrite = System.currentTimeMillis();
			config.writeJar(service, impl, true);
			impl.close();
			System.out.println("Impl finished in " + (System.currentTimeMillis() - startWrite) + "ms!");
		} catch (Throwable t) {
			t.printStackTrace();
		}
		service.shutdown();
		service.awaitTermination(100, TimeUnit.SECONDS);
	}
}
