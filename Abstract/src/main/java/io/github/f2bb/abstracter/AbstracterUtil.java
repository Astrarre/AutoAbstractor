package io.github.f2bb.abstracter;

import static io.github.f2bb.abstracter.AbstracterConfig.registerConstants;
import static io.github.f2bb.abstracter.AbstracterConfig.registerInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.zip.ZipOutputStream;

import io.github.f2bb.abstracter.abs.BaseAbstracter;
import io.github.f2bb.abstracter.abs.ConstantsAbstracter;
import io.github.f2bb.abstracter.abs.InterfaceAbstracter;
import io.github.f2bb.abstracter.func.elements.FieldSupplier;
import io.github.f2bb.abstracter.func.filter.MemberFilter;
import io.github.f2bb.abstracter.util.AbstracterLoader;
import io.github.f2bb.abstracter.decompiler.Decompile;

public class AbstracterUtil {
	public static String pkg = "/io/github/f2bb/";
	public static void apply(List<File> classpath, String apiFile, String sourcesFile, String implFile, String manifestFile, String mappingsFile) {
		try {
			System.out.println("Writing api...");
			ZipOutputStream api = new ZipOutputStream(new FileOutputStream(apiFile));
			AbstracterConfig.writeJar(api, false);
			api.close();

			System.out.println("Writing impl...");
			ZipOutputStream impl = new ZipOutputStream(new FileOutputStream(implFile));
			AbstracterConfig.writeJar(impl, true);
			impl.close();

			System.out.println("Writing manifest...");
			FileOutputStream manifest = new FileOutputStream(manifestFile);
			AbstracterConfig.writeManifest(manifest);
			manifest.close();

			System.out.println("Decompiling api for api sources...");
			Decompile.decompile(classpath, new File(apiFile), new File(sourcesFile), new File(mappingsFile));
		} catch (IOException e) {
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

	public static void registerDefaultInterface(Class<?>... cls) {
		for (Class<?> cl : cls) {
			registerInterface(cl, InterfaceAbstracter::new);
		}
	}

	public static void registerDefaultConstants(Class<?>... cls) {
		for (Class<?> cl : cls) {
			registerConstants(cl, ConstantsAbstracter::new);
		}
	}

	public static void registerConstantlessInterface(Class<?>...cls) {
		for (Class<?> cl : cls) {
			registerInterface(cl, c -> new InterfaceAbstracter(c).fields(FieldSupplier.INTERFACE_DEFAULT.filtered((MemberFilter)
					MemberFilter.PUBLIC.and(MemberFilter.STATIC).negate())));
		}
	}

	public static void registerDefaultBase(Class<?>... base) {
		for (Class<?> aClass : base) {
			AbstracterConfig.registerBase(aClass, BaseAbstracter::new);
		}
	}

	private static URL toURL(URI u) {
		try {
			return u.toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
}
