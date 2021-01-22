package io.github.astrarre.abstracter;


import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipOutputStream;

import org.zeroturnaround.zip.ZipUtil;

import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;
import net.fabricmc.mappingpoet.Main;
import net.fabricmc.mappingpoet.ManifestRemapper;
import net.fabricmc.mappingpoet.RemappingMappingsStore;

public class AbstracterUtil {
	public static String pkg = "io/github/astrarre/v%d/";
	private final String api, source, impl;
	private final TinyTree mappings;
	private final List<URL> classpath;
	private final URL minecraft;

	public AbstracterUtil(String api, String source, String impl, TinyTree mappings, List<URL> classpath, URL minecraft) {
		this.api = api;
		this.source = source;
		this.impl = impl;
		this.mappings = mappings;
		this.classpath = classpath;
		this.minecraft = minecraft;
	}

	public static AbstracterUtil fromFile(String args) throws IOException {
		Properties properties = new Properties();
		properties.load(new FileReader(args));
		String minecraft = properties.getProperty("minecraft");

		List<URL> classpath = new ArrayList<>();
		for (String s : properties.getProperty("libraries").split(";")) {
			classpath.add(new File(s).toURI().toURL());
		}

		return new AbstracterUtil(properties.getProperty("api_jar"),
				properties.getProperty("api_sources_jar"),
				properties.getProperty("impl_jar"),
				TinyMappingFactory.loadWithDetection(Files.newBufferedReader(new File(properties.getProperty("mappings")).toPath())),
				classpath,
				new File(minecraft).toURI().toURL());
	}

	public AbstracterConfig createConfig(String dir) {
		AbstracterConfig config = new AbstracterConfig(dir, this.mappings);
		this.classpath.forEach(config.classpath::addURL);
		config.minecraft.addURL(this.minecraft);
		return config;
	}

	public void write(AbstracterConfig config) throws InterruptedException {
		ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);
		service.submit(() -> {
			try {
				System.out.println("Writing api...");
				ZipOutputStream api = new ZipOutputStream(new FileOutputStream(this.api));
				long startWrite = System.currentTimeMillis();
				config.writeJar(service, api, false);
				api.close();
				System.out.println("Api finished in " + (System.currentTimeMillis() - startWrite) + "ms! Now decompiling...");

				startWrite = System.currentTimeMillis();

				Path dir = Files.createTempDirectory("decomp");
				Main.generate(Paths.get(this.api), dir, new RemappingMappingsStore(this.mappings, new ManifestRemapper(config.nameMap())));
				System.out.println("Generated sources in " + (System.currentTimeMillis() - startWrite) + "ms! Now packing sources into zip...");
				startWrite = System.currentTimeMillis();
				ZipUtil.pack(dir.toFile(), Paths.get(this.source).toFile());
				System.out.println("Packed zip in " + (System.currentTimeMillis() - startWrite) + "ms! Finished api export!");
				return null;
			} catch (Throwable t) {
				t.printStackTrace();
				return null;
			}
		});

		try {
			System.out.println("Writing impl...");
			ZipOutputStream impl = new ZipOutputStream(new FileOutputStream(this.impl));
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
