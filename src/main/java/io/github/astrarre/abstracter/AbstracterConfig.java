package io.github.astrarre.abstracter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

import net.fabricmc.mapping.tree.ClassDef;
import net.fabricmc.mapping.tree.MethodDef;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;

public class AbstracterConfig implements Opcodes {
	// isolated classloader
	public final AbstracterLoader classpath = new AbstracterLoader(ClassLoader.getSystemClassLoader().getParent());
	public final AbstracterLoader minecraft = new AbstracterLoader(this.classpath);
	private final Map<String, AbstractAbstracter> interfaceAbstractions = new HashMap<>();
	private final Map<String, AbstractAbstracter> baseAbstractions = new HashMap<>();
	private final Map<String, ClassDef> classes = new HashMap<>();
	private final Map<Entry, Optional<MethodDef>> cache = new HashMap<>();

	public AbstracterConfig(Path mappings) throws IOException {
		this(TinyMappingFactory.loadWithDetection(Files.newBufferedReader(mappings)));
	}

	public AbstracterConfig(TinyTree mappings) {
		for (ClassDef cls : mappings.getClasses()) {
			this.classes.put(cls.getName("named"), cls);
		}
	}

	public void writeJar(ExecutorService executor, ZipOutputStream out, boolean impl) throws IOException, InterruptedException, ExecutionException {
		List<Callable<ClassNode>> queue = new Vector<>();
		this.interfaceAbstractions.entrySet().parallelStream().forEach((e) -> queue.add(() -> e.getValue().apply(this, impl)));
		this.baseAbstractions.entrySet().parallelStream().forEach((e) -> queue.add(() -> e.getValue().apply(this, impl)));

		for (Future<ClassNode> future : executor.invokeAll(queue, 5, TimeUnit.MINUTES)) {
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			ClassNode node = future.get();
			node.accept(writer);
			out.putNextEntry(new ZipEntry(node.name + ".class"));
			out.write(writer.toByteArray());
			out.closeEntry();
		}

		if (impl) {
			out.putNextEntry(new ZipEntry("intr_manifest.properties"));
			this.writeManifest(out);
			out.closeEntry();
		}
	}

	public void writeManifest(OutputStream stream) throws IOException {
		Properties properties = new Properties();
		this.interfaceAbstractions.forEach((c, a) -> properties.setProperty(c, a.name));
		this.baseAbstractions.forEach((c, a) -> properties.setProperty(c, a.name));
		properties.store(stream, "F2bb Interface Manifest");
		// todo remap
	}

	public void manualInterface(Class<?> mcClass, String abstraction) {
		this.registerInterface(new ManualAbstracter(mcClass, abstraction));
	}

	public AbstractAbstracter registerInterface(AbstractAbstracter abstracter) {
		this.interfaceAbstractions.put(abstracter.cls, abstracter);
		return abstracter;
	}

	public AbstractAbstracter registerInterface(Class<?> cls) {
		return this.registerInterface(new InterfaceAbstracter(cls));
	}

	public AbstractAbstracter registerBase(Class<?> cls) {
		return this.registerBase(new BaseAbstracter(cls));
	}

	public AbstractAbstracter registerBase(AbstractAbstracter abstracter) {
		this.baseAbstractions.put(abstracter.cls, abstracter);
		return abstracter;
	}

	public AbstractAbstracter getInterfaceAbstraction(String internalName) {
		return this.interfaceAbstractions.get(internalName);
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

	public Class<?> getClass(String internalName) {
		try {
			return Class.forName(internalName.replace('/', '.'), false, this.minecraft);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
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

	public MethodDef getMethod(Method method) {
		return this.search(method.getDeclaringClass(), method.getName(), Type.getMethodDescriptor(method));
	}

	private MethodDef search(Class<?> cls, String name, String desc) {
		if (cls == null) {
			return null;
		}

		return this.cache.computeIfAbsent(new Entry(cls, name, desc), entry -> {
			ClassDef def = this.classes.get(Type.getInternalName(cls));
			if(def == null) return Optional.empty();
			for (MethodDef method : def.getMethods()) {
				if (method.getName("named").equals(name) && method.getDescriptor("named").equals(desc)) {
					return Optional.of(method);
				}
			}

			MethodDef current = this.search(cls.getSuperclass(), name, desc);
			if (current != null) {
				return Optional.of(current);
			}

			for (Class<?> iface : cls.getInterfaces()) {
				current = this.search(iface, name, desc);
				if (current != null) {
					return Optional.of(current);
				}
			}
			return Optional.empty();
		}).orElse(null);
	}

	private static final class Entry {
		private final Class<?> cls;
		private final String name, desc;

		private Entry(Class<?> cls, String name, String desc) {
			this.cls = cls;
			this.name = name;
			this.desc = desc;
		}

		@Override
		public int hashCode() {
			int result = this.cls != null ? this.cls.hashCode() : 0;
			result = 31 * result + (this.name != null ? this.name.hashCode() : 0);
			result = 31 * result + (this.desc != null ? this.desc.hashCode() : 0);
			return result;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof Entry)) {
				return false;
			}

			Entry entry = (Entry) object;

			if (!Objects.equals(this.cls, entry.cls)) {
				return false;
			}
			if (!Objects.equals(this.name, entry.name)) {
				return false;
			}
			return Objects.equals(this.desc, entry.desc);
		}
	}
}
