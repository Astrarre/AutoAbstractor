package io.github.f2bb.stripper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class StripAsm {
	public static void main(String[] args) throws IOException {
		ZipFile file = new ZipFile(args[0]);
		Enumeration<? extends ZipEntry> enumeration = file.entries();
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(args[1]));
		while (enumeration.hasMoreElements()) {
			ZipEntry entry = enumeration.nextElement();
			out.putNextEntry(entry);
			if(entry.getName().endsWith(".class")) {
				ClassReader reader = new ClassReader(file.getInputStream(entry));
				ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			}
			out.closeEntry();
		}
		out.close();
	}

	public static boolean keep(int access) {
		return (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0;
	}
}
