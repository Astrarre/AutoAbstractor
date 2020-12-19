package io.github.astrarre.stripper.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ByteMapOutputStreamWriter implements ZipWriter {
	private final Map<String, ByteArrayOutputStream> toWrite = new HashMap<>();
	private String entry;
	private final ZipOutputStream out;

	public ByteMapOutputStreamWriter(File file) throws FileNotFoundException {
		this(new ZipOutputStream(new FileOutputStream(file)));
	}

	public ByteMapOutputStreamWriter(ZipOutputStream out) {this.out = out;}

	@Override
	public void openEntry(String name) {
		this.entry = name;
		this.toWrite.put(name, new ByteArrayOutputStream());
	}

	@Override
	public OutputStream getOutput() {
		return this.toWrite.get(this.entry);
	}

	@Override public void closeEntry() {}

	@Override
	public void close() {
		this.toWrite.forEach((s, b) -> {
			try {
				this.out.putNextEntry(new ZipEntry(s));
				b.writeTo(this.out);
				this.out.closeEntry();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		try {
			this.out.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
