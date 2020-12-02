package io.github.astrarre.stripper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import io.github.astrarre.Hide;
import io.github.astrarre.stripper.asm.EnumStripper;
import io.github.astrarre.stripper.asm.ElementStripper;
import io.github.astrarre.stripper.java.JavaStripper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class Stripper {
	public static void main(String[] args) throws IOException {
		strip(Collections.emptyList(), new File("fodder.jar"), new File("out_fodder.jar"));
	}

	/**
	 * todo add in the api jar
	 *
	 * @param filter a list of regexes to filter out files or folders, the pattern must match both .class and .java!
	 * @param inputFile the input jar
	 * @param outputFile the output jar
	 */
	@Hide
	public static void strip(List<String> filter, File inputFile, File outputFile) throws IOException {
		Predicate<String> patterns = filter.stream().map(Pattern::compile).map(Pattern::asPredicate)
		                           .reduce(Predicate::and).orElse(s -> false);

		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile));
		ZipFile file = new ZipFile(inputFile);
		Enumeration<? extends ZipEntry> enumeration = file.entries();
		byte[] buffer = new byte[4096];

		while (enumeration.hasMoreElements()) {
			ZipEntry entry = enumeration.nextElement();
			String name = entry.getName();

			if(patterns.test(name)) {
				continue;
			}

			InputStream input = file.getInputStream(entry);
			if (name.endsWith(".class")) {
				// asm strip
				ClassReader reader = new ClassReader(input);
				ClassNode node = new ClassNode();
				reader.accept(node, ClassReader.SKIP_FRAMES);
				if(node.outerMethod == null) {
					ElementStripper.strip(node);
					EnumStripper.strip(node);
					ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					node.accept(writer);

					zos.putNextEntry(new ZipEntry(entry.getName()));
					zos.write(writer.toByteArray());
					zos.closeEntry();
				}
			} else if (name.endsWith(".java")) {
				// source strip
				CompilationUnit unit = StaticJavaParser.parse(input);
				JavaStripper.stripAnnotations(unit);
				JavaStripper.stripInaccessibles(unit);
				JavaStripper.nukeImplementation(unit);
				zos.putNextEntry(new ZipEntry(entry.getName()));
				Writer writer = new OutputStreamWriter(zos);
				writer.write(unit.toString());
				writer.flush();
				zos.closeEntry();
			} else {
				zos.putNextEntry(entry);
				InputStream stream = file.getInputStream(entry);
				int i;
				while ((i = stream.read(buffer)) != -1) {
					zos.write(buffer, 0, i);
				}
				zos.closeEntry();
			}
		}

		zos.close();
	}
}
