package io.github.f2bb;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipOutputStream;

import io.github.f2bb.abstraction.AbstractAbstracter;
import io.github.f2bb.abstraction.base.AsmBaseAbstracter;
import io.github.f2bb.abstraction.base.JavaBaseAbstracter;
import io.github.f2bb.loader.AbstracterLoaderImpl;

public class AutoAbstracter {
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		new File("output.jar").delete();
		List<URL> classpath = new ArrayList<>();
		for (File file : Objects.requireNonNull(new File("classpath").listFiles())) {
			classpath.add(file.toURI().toURL());
		}
		classpath.add(new File("fodder.jar").toURI().toURL());

		AbstracterLoaderImpl loader = new AbstracterLoaderImpl(classpath.toArray(new URL[0]));
		AbstractAbstracter javaAbstracter = new JavaBaseAbstracter(loader,
				loader.getClass("net.minecraft.block.Block"));
		AbstractAbstracter asmAbstracter = new AsmBaseAbstracter(loader,
				loader.getClass("net.minecraft.block.Block"), false);

		ZipOutputStream out = new ZipOutputStream(new FileOutputStream("output.jar"));
		javaAbstracter.write(out);
		asmAbstracter.write(out);
		out.close();
	}
}
