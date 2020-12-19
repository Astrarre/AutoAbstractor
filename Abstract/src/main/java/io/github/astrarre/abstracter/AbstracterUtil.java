package io.github.astrarre.abstracter;

import static io.github.astrarre.abstracter.AbstracterConfig.registerConstants;
import static io.github.astrarre.abstracter.AbstracterConfig.registerInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipOutputStream;

import io.github.astrarre.abstracter.abs.BaseAbstracter;
import io.github.astrarre.abstracter.abs.ConstantsAbstracter;
import io.github.astrarre.abstracter.abs.InterfaceAbstracter;
import io.github.astrarre.abstracter.func.elements.FieldSupplier;
import io.github.astrarre.abstracter.func.filter.MemberFilter;
import io.github.astrarre.abstracter.util.AbstracterLoader;
import org.zeroturnaround.zip.ZipUtil;

import net.fabricmc.mappingpoet.Main;

public class AbstracterUtil {
	public static String pkg = "/io/github/astrarre/";

	public static void applyParallel(String apiFile, String sourcesFile, String implFile, String manifestFile, String mappingsFile) {
		ExecutorService service = Executors.newFixedThreadPool(2);
		service.submit(() -> {
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
			return null;
		});
		service.submit(() -> {
			System.out.println("Writing impl...");
			ZipOutputStream impl = new ZipOutputStream(new FileOutputStream(implFile));
			AbstracterConfig.writeJar(impl, true);
			impl.close();
			System.out.println("Impl finished!");
			return null;
		});
		service.shutdown();
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
			reader.lines().map(File::new).map(File::toURI).map(AbstracterUtil::toURL).forEach(AbstracterLoader.CLASSPATH::addURL);
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

	public static void registerDefaultConstants(Class<?>... cls) {
		for (Class<?> cl : cls) {
			registerConstants(new ConstantsAbstracter(cl));
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
