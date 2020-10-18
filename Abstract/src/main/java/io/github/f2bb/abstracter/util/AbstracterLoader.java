package io.github.f2bb.abstracter.util;

import java.net.URL;
import java.net.URLClassLoader;

public class AbstracterLoader extends URLClassLoader {
	public AbstracterLoader() {
		super(new URL[] {});
	}

	public AbstracterLoader(ClassLoader parent) {
		super(new URL[] {}, parent);
	}

	@Override
	public void addURL(URL url) {
		super.addURL(url);
	}
}
