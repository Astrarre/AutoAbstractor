package io.github.astrarre.abstracter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import io.github.astrarre.abstracter.abs.BaseAbstracter;
import io.github.astrarre.abstracter.abs.InterfaceAbstracter;
import io.github.astrarre.abstracter.abs.ManualAbstracter;
import io.github.astrarre.abstracter.ex.InvalidClassException;
import io.github.astrarre.abstracter.util.AbstracterLoader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public class AbstracterConfig implements Opcodes {
	// isolated classloader
	public final AbstracterLoader classpath = new AbstracterLoader(ClassLoader.getSystemClassLoader().getParent());
	public final AbstracterLoader minecraft = new AbstracterLoader(this.classpath);
	private final Map<String, AbstractAbstracter> interfaceAbstractions = new HashMap<>();
	private final Map<String, AbstractAbstracter> baseAbstractions = new HashMap<>();

	public void writeManifest(OutputStream stream) throws IOException {
		Properties properties = new Properties();
		this.interfaceAbstractions.forEach((c, a) -> properties.setProperty(c, a.name));
		properties.store(stream, "F2bb Interface Manifest");
		// todo remap
	}

	public void writeJar(ZipOutputStream out, boolean impl) throws IOException {
		this.write(out, this.interfaceAbstractions, impl);
		this.write(out, this.baseAbstractions, impl);
		if (impl) {
			out.putNextEntry(new ZipEntry("intr_manifest.properties"));
			this.writeManifest(out);
			out.closeEntry();
		}
	}

	private void write(ZipOutputStream out, Map<String, AbstractAbstracter> abstraction, boolean impl) {
		Map<String, ClassNode> cache = new HashMap<>();
		abstraction.forEach((cls, abs) -> {
			ClassNode node = cache.computeIfAbsent(cls, c -> abstraction.get(c).apply(this, impl));
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			node.accept(writer);
			try {
				out.putNextEntry(new ZipEntry(node.name + ".class"));
				out.write(writer.toByteArray());
				out.closeEntry();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	public void manualInterface(Class<?> mcClass, String abstraction) {
		this.registerInterface(new ManualAbstracter(mcClass, abstraction));
	}

	public AbstractAbstracter registerInterface(AbstractAbstracter abstracter) {
		this.interfaceAbstractions.put(abstracter.cls, abstracter);
		return abstracter;
	}

	public AbstractAbstracter registerBase(AbstractAbstracter abstracter) {
		this.baseAbstractions.put(abstracter.cls, abstracter);
		return abstracter;
	}

	public AbstractAbstracter registerInterface(Class<?> cls) {
		return this.registerInterface(new InterfaceAbstracter(cls));
	}

	public AbstractAbstracter registerBase(Class<?> cls) {
		return this.registerInterface(new BaseAbstracter(cls));
	}

	public AbstractAbstracter getInterfaceAbstraction(String internalName) {
		return this.interfaceAbstractions.get(internalName);
	}

	public Class<?> getClass(String internalName) {
		try {
			return Class.forName(internalName.replace('/', '.'), false, this.minecraft);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public String getInterfaceName(Class<?> cls) {
		String cls1 = Type.getInternalName(cls);
		AbstractAbstracter abstraction = this.interfaceAbstractions.get(cls1);
		if (abstraction == null) {
			Class<?> c = this.getClass(cls1);
			if (this.isMinecraft(c)) {
				throw new InvalidClassException(c);
			} else {
				return cls1;
			}
		}

		return abstraction.name;
	}

	public boolean isMinecraft(Class<?> cls) {
		return cls != null && cls.getClassLoader() == this.minecraft;
	}

	/**
	 * @return abstracted class name -> minecraft class name
	 */
	public Map<String, String> nameMap() {
		// todo reverse this, add custom mappings soon tm
		Map<String, String> map = new HashMap<>();
		this.baseAbstractions.forEach((k, a) -> map.put(k, a.name));
		this.interfaceAbstractions.forEach((k, a) -> map.put(k, a.name));
		return map;
	}

	/**
	 * @return true if the class is a minecraft class, but isn't supposed to be abstracted
	 */
	public boolean isUnabstractedClass(Class<?> cls) {
		return this.isMinecraft(cls) && !this.isInterfaceAbstracted(cls);
	}

	public boolean isInterfaceAbstracted(Class<?> cls) {
		return this.isInterfaceAbstracted(Type.getInternalName(cls));
	}

	public boolean isInterfaceAbstracted(String internalName) {
		return this.interfaceAbstractions.containsKey(internalName);
	}
}
