package io.github.astrarre.abstracter;

import static io.github.astrarre.abstracter.AbstracterConfig.registerInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipOutputStream;

import io.github.astrarre.abstracter.abs.BaseAbstracter;
import io.github.astrarre.abstracter.abs.InterfaceAbstracter;
import io.github.astrarre.abstracter.func.elements.FieldSupplier;
import io.github.astrarre.abstracter.func.filter.MemberFilter;
import io.github.astrarre.abstracter.util.AbstracterLoader;
import org.zeroturnaround.zip.ZipUtil;

import net.fabricmc.mappingpoet.Main;

public class AbstracterUtil {
	public static String pkg = "io/github/astrarre/v%d/";

	public static void applyParallel(String args, Runnable runnable) throws IOException, InterruptedException {
		Properties properties = new Properties();
		properties.load(new FileReader(args));
		String mappings = properties.getProperty("mappings"), minecraft = properties.getProperty("minecraft"), libraries = properties.getProperty(
				"libraries"), api_jar = properties.getProperty("api_jar"), api_sources_jar = properties.getProperty("api_sources_jar"), impl_jar = properties
						                                                                                                                                   .getProperty(
								                                                                                                                                   "impl_jar");

		for (String library : libraries.split(";")) {
			File file = new File(library);
			AbstracterConfig.CLASSPATH.addURL(file.toURI().toURL());
		}

		AbstracterConfig.INSTANCE.addURL(new File(minecraft).toURI().toURL());

		runnable.run();

		applyParallel(api_jar, api_sources_jar, impl_jar, mappings);
	}

	public static void applyParallel(String apiFile, String sourcesFile, String implFile, String mappingsFile) throws InterruptedException {
		ExecutorService service = Executors.newFixedThreadPool(2);
		service.submit(() -> {
			try {
				System.out.println("Writing api...");
				ZipOutputStream api = new ZipOutputStream(new FileOutputStream(apiFile));
				AbstracterConfig.writeJar(api, false);
				api.close();
				System.out.println("Api finished!");

				System.out.println("Decompiling api for api sources...");
				Path dir = Files.createTempDirectory("decomp");
				Main.generate(Paths.get(mappingsFile), Paths.get(apiFile), dir, AbstracterConfig.nameMap());
				System.out.println("Packing sources...");
				ZipUtil.pack(dir.toFile(), Paths.get(sourcesFile).toFile());
				System.out.println("done!");
				return null;
			} catch (Throwable t) {
				t.printStackTrace();
				return null;
			}
		});
		service.submit(() -> {
			try {
				System.out.println("Writing impl...");
				ZipOutputStream impl = new ZipOutputStream(new FileOutputStream(implFile));
				AbstracterConfig.writeJar(impl, true);
				impl.close();
				System.out.println("Impl finished!");
				return null;
			} catch (Throwable t) {
				t.printStackTrace();
				return null;
			}
		});
		service.shutdown();
		service.awaitTermination(100, TimeUnit.SECONDS);
	}

	public static void apply(String apiFile, String sourcesFile, String implFile, String manifestFile, String mappingsFile) {
		try {
			System.out.println("Writing api...");
			ZipOutputStream api = new ZipOutputStream(new FileOutputStream(apiFile));
			AbstracterConfig.writeJar(api, false);
			api.close();
			System.out.println("Api finished!");

			System.out.println("Writing manifest...");
			FileOutputStream manifest = new FileOutputStream(manifestFile);
			AbstracterConfig.writeManifest(manifest);
			manifest.close();
			System.out.println("Manifest finished!");

			System.out.println("Decompiling api for api sources...");
			Path dir = Files.createTempDirectory("decomp");
			Main.generate(Paths.get(mappingsFile), Paths.get(apiFile), dir, Paths.get(manifestFile));
			ZipUtil.pack(dir.toFile(), Paths.get(sourcesFile).toFile());
			System.out.println("Decompiled!");

			System.out.println("Writing impl...");
			ZipOutputStream impl = new ZipOutputStream(new FileOutputStream(implFile));
			AbstracterConfig.writeJar(impl, true);
			impl.close();
			System.out.println("Impl finished!");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void loadFromTxt(File file) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			reader.lines().map(File::new).map(File::toURI).map(AbstracterUtil::toURL).forEach(AbstracterConfig.CLASSPATH::addURL);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private static URL toURL(URI u) {
		try {
			return u.toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public static void registerDefaultInterface(Class<?>... cls) {
		for (Class<?> cl : cls) {
			registerInterface(new InterfaceAbstracter(cl));
		}
	}

	public static void registerConstantlessInterface(Class<?>... cls) {
		for (Class<?> cl : cls) {
			registerInterface(new InterfaceAbstracter(cl).fields(FieldSupplier.INTERFACE_DEFAULT.filtered((MemberFilter) MemberFilter.PUBLIC.and(
					MemberFilter.STATIC).negate())));
		}
	}

	public static void registerDefaultBase(Class<?>... base) {
		for (Class<?> aClass : base) {
			AbstracterConfig.registerBase(new BaseAbstracter(aClass));
		}
	}
}
