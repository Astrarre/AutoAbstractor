package io.github.astrarre.abstracter.util;

import java.net.URL;
import java.net.URLClassLoader;

import io.github.astrarre.abstracter.AbstracterConfig;

public class AbstracterLoader extends URLClassLoader {
	static {
		registerAsParallelCapable();
	}

	public AbstracterLoader(ClassLoader parent) {
		super(new URL[] {}, parent);
	}

	@Override
	public void addURL(URL url) {
		super.addURL(url);
	}
}
