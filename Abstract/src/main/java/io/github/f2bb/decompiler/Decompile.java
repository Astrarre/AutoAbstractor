package io.github.f2bb.decompiler;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.github.f2bb.decompiler.fernflower.AbstracterTinyJavadocProvider;
import io.github.f2bb.decompiler.fernflower.ThreadIDFFLogger;
import io.github.f2bb.decompiler.fernflower.ThreadSafeResultSaver;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.util.InterpreterUtil;

import net.fabricmc.fernflower.api.IFabricJavadocProvider;

public class Decompile {
	public static void decompile(List<File> classpath, File input, File output, File lineMap, File mappings) {
		Map<String, Object> options = new HashMap<>();
		options.put(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES, "1");
		options.put(IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1");
		options.put(IFernflowerPreferences.REMOVE_SYNTHETIC, "1");
		options.put(IFernflowerPreferences.LOG_LEVEL, "trace");
		options.put(IFernflowerPreferences.THREADS, Runtime.getRuntime().availableProcessors()-1);
		options.put(IFabricJavadocProvider.PROPERTY_NAME, new AbstracterTinyJavadocProvider(mappings));
		Fernflower fernflower = new Fernflower(Decompile::getBytecode, new ThreadSafeResultSaver(() -> output, () -> lineMap), options, new ThreadIDFFLogger());
		classpath.forEach(fernflower::addLibrary);
		fernflower.addSource(input);
		fernflower.decompileContext();
	}

	public static byte[] getBytecode(String externalPath, String internalPath) throws IOException {
		File file = new File(externalPath);

		if (internalPath == null) {
			return InterpreterUtil.getBytes(file);
		} else {
			try (ZipFile archive = new ZipFile(file)) {
				ZipEntry entry = archive.getEntry(internalPath);

				if (entry == null) {
					throw new IOException("Entry not found: " + internalPath);
				}

				return InterpreterUtil.getBytes(archive, entry);
			}
		}
	}
}
