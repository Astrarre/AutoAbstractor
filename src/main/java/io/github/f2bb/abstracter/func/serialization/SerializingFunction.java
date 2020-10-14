package io.github.f2bb.abstracter.func.serialization;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.squareup.javapoet.TypeSpec;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public interface SerializingFunction<T> {
	SerializingFunction<ClassNode> ASM = (s, o, t) -> {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		t.accept(writer);
		s.putNextEntry(new ZipEntry(t.name + ".class"));
		s.write(writer.toByteArray());
		s.closeEntry();
	};

	SerializingFunction<TypeSpec.Builder> JAVA = (s, o, t) -> {
		TypeSpec built = t.build();
		s.putNextEntry(new ZipEntry(built.name + ".java"));
		OutputStreamWriter writer = new OutputStreamWriter(s);
		writer.write(built.toString());
		writer.flush();
		s.closeEntry();
	};

	void serialize(ZipOutputStream stream, Class<?> original, T object) throws IOException;
}
