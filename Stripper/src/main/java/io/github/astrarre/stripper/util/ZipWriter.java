package io.github.astrarre.stripper.util;

import java.io.OutputStream;

public interface ZipWriter {
	void openEntry(String name);
	OutputStream getOutput();
	void closeEntry();

	void close();
}
