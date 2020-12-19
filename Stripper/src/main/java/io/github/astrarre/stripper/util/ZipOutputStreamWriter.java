package io.github.astrarre.stripper.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipOutputStreamWriter implements ZipWriter {
	private final ZipOutputStream out;

	public ZipOutputStreamWriter(File file) throws FileNotFoundException {
		this(new ZipOutputStream(new FileOutputStream(file)));
	}

	public ZipOutputStreamWriter(ZipOutputStream out) {this.out = out;}

	@Override
	public void openEntry(String name) {
		try {
			this.out.putNextEntry(new ZipEntry(name));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public OutputStream getOutput() {
		return this.out;
	}

	@Override
	public void closeEntry() {
		try {
			this.out.closeEntry();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() {
		try {
			this.out.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
