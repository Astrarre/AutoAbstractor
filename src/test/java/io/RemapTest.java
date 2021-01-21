package io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.io.Files;

import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

public class RemapTest {
	public static void main(String[] args) throws IOException, InterruptedException {
		AbstractTest.main(args);
		TinyRemapper remapper = TinyRemapper.newRemapper()
		                                    .withMappings(TinyUtils.createTinyMappingProvider(Paths.get("mappings.tiny"), "named", "intermediary"))
		                                    .ignoreConflicts(true)
		                                    //.resolveMissing(true)
		                                    .threads(4)
		                                    .build();
		BufferedReader reader = Files.newReader(new File("classpath.txt"), StandardCharsets.UTF_8);
		remapper.readClassPathAsync(reader.lines().map(Paths::get).toArray(Path[]::new));
		remapper.readClassPathAsync(Paths.get("fodder.jar"));
		remapper.readInputs(Paths.get("impl.jar"));

		try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(Paths.get("impl_inter.jar")).build()) {
			remapper.apply(outputConsumer);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			remapper.finish();
		}
	}
}
