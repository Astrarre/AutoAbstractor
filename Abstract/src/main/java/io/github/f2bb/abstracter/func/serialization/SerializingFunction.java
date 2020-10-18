package io.github.f2bb.abstracter.func.serialization;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import io.github.f2bb.abstracter.AbstracterConfig;
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

	static SerializingFunction<TypeSpec.Builder> getJava(boolean isInterface) {
		return (s, o, t) -> {
			if(o.getEnclosingClass() == null) {
				TypeSpec built = t.build();
				// oh shit
				String internal = isInterface ? AbstracterConfig.getInterfaceName(o) : AbstracterConfig.getBaseName(o);
				s.putNextEntry(new ZipEntry(internal + ".java"));
				JavaFile file = JavaFile.builder(getPackage(internal), built).build();
				OutputStreamWriter writer = new OutputStreamWriter(s);
				file.writeTo(writer);
				writer.flush();
				s.closeEntry();
			}
		};
	}

	static <T> SerializingFunction<T> nothing() {
		return (s, o, t) -> {};
	}

	void serialize(ZipOutputStream stream, Class<?> original, T object) throws IOException;

	static String getPackage(String internalName) {
		int index = internalName.lastIndexOf('/');
		return internalName.substring(0, index == -1 ? 0 : index).replace('/', '.');
	}
}
