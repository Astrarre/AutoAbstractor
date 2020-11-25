package io.github.astrarre.abstracter.decompiler.fernflower;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.abs.AbstractAbstracter;
import io.github.astrarre.abstracter.util.AbstracterLoader;
import io.github.astrarre.abstracter.util.reflect.ReflectUtil;
import org.jetbrains.java.decompiler.modules.decompiler.exps.AnnotationExprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.ConstExprent;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.attr.StructAnnotationAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.objectweb.asm.commons.Remapper;

import net.fabricmc.fernflower.api.IFabricJavadocProvider;
import net.fabricmc.mapping.tree.ClassDef;
import net.fabricmc.mapping.tree.FieldDef;
import net.fabricmc.mapping.tree.MethodDef;
import net.fabricmc.mapping.tree.ParameterDef;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;
import net.fabricmc.mappings.EntryTriple;

@SuppressWarnings ("unchecked")
public class AbstracterTinyJavadocProvider implements IFabricJavadocProvider {
	private static final Remapper REMAPPER = new Remapper() {
		private final Map<String, String> map = AbstracterConfig.nameMap();

		@Override
		public String map(String internalName) {
			return this.map.getOrDefault(internalName, internalName);
		}
	};

	private final Map<String, ClassDef> classes = new HashMap<>();
	private final Map<EntryTriple, FieldDef> fields = new HashMap<>();
	private final Map<EntryTriple, MethodDef> methods = new HashMap<>();
	private final String namespace = "named";

	public AbstracterTinyJavadocProvider(File tinyFile) {
		TinyTree result;
		try (BufferedReader reader = Files.newBufferedReader(tinyFile.toPath())) {
			result = TinyMappingFactory.loadWithDetection(reader);
		} catch (IOException e) {
			throw new RuntimeException("Failed to read mappings", e);
		}

		final TinyTree mappings = result;

		for (ClassDef classDef : mappings.getClasses()) {
			final String className = classDef.getName(this.namespace);
			this.classes.put(className, classDef);

			for (FieldDef fieldDef : classDef.getFields()) {
				this.fields.put(new EntryTriple(className,
						fieldDef.getName(this.namespace),
						fieldDef.getDescriptor(this.namespace)), fieldDef);
			}

			for (MethodDef methodDef : classDef.getMethods()) {
				this.methods.put(new EntryTriple(className,
						methodDef.getName(this.namespace),
						methodDef.getDescriptor(this.namespace)), methodDef);
			}
		}
	}

	@Override
	public String getClassDoc(StructClass structClass) {
		ClassDef classDef = this.classes.get(REMAPPER.map(structClass.qualifiedName));
		return classDef != null ? classDef.getComment() : null;
	}

	@Override
	public String getFieldDoc(StructClass structClass, StructField structField) {
		return this.getFieldDoc(REMAPPER.map(structClass.qualifiedName),
				structField.getName(),
				REMAPPER.mapDesc(structField.getDescriptor()));
	}

	private String getFieldDoc(String owner, String name, String type) {
		FieldDef fieldDef = this.fields.get(new EntryTriple(owner, name, type));
		return fieldDef != null ? fieldDef.getComment() : null;
	}


	@Override
	public String getMethodDoc(StructClass structClass, StructMethod structMethod) {
		if (structMethod.hasAttribute(StructGeneralAttribute.ATTRIBUTE_RUNTIME_INVISIBLE_ANNOTATIONS)) {
			StructAnnotationAttribute attribute = structMethod
					                                      .getAttribute(StructGeneralAttribute.ATTRIBUTE_RUNTIME_INVISIBLE_ANNOTATIONS);
			List<AnnotationExprent> annos = attribute.getAnnotations();
			for (AnnotationExprent anno : annos) {
				if (AbstractAbstracter.FIELD_REF_NAME.equals(anno.getClassName())) {
					Map<String, ConstExprent> vals = (Map) ReflectUtil.getValues(anno);
					String owner = (String) vals.get("owner").getValue();
					String name = (String) vals.get("name").getValue();
					String type = (String) vals.get("type").getValue();
					return this.getFieldDoc(owner, name, type);
				}
			}
		}

		String owner = REMAPPER.map(structClass.qualifiedName);
		String name = structMethod.getName();
		String desc = REMAPPER.mapMethodDesc(structMethod.getDescriptor());
		return this.getMethodDocRecurse(owner, name, desc);
	}

	private String getMethodDoc(String owner, String name, String desc) {
		MethodDef methodDef = this.methods.get(new EntryTriple(owner, name, desc));
		if (methodDef != null) {
			List<String> parts = new ArrayList<>();

			if (methodDef.getComment() != null) {
				parts.add(methodDef.getComment());
			}

			boolean addedParam = false;

			for (ParameterDef param : methodDef.getParameters()) {
				String comment = param.getComment();

				if (comment != null) {
					if (!addedParam && methodDef.getComment() != null) {
						//Add a blank line before params when the method has a comment
						parts.add("");
						addedParam = true;
					}

					parts.add(String.format("@param %s %s", param.getName(this.namespace), comment));
				}
			}

			if (parts.isEmpty()) {
				return null;
			}

			return String.join("\n", parts);
		}

		return null;
	}

	private String getMethodDocRecurse(String owner, String name, String desc) {
		String doc = this.getMethodDoc(owner.replace('.', '/'), name, desc);
		if (doc != null) {
			return doc;
		}
		Class<?> cls = AbstracterLoader.getClass(owner.replace('/', '.'));
		if (!AbstracterLoader.isMinecraft(cls)) {
			return null;
		}

		// todo deal with bridge methods
		Class<?> sup = cls.getSuperclass();
		if (sup != null) {
			doc = this.getMethodDocRecurse(sup.getName(), name, desc);
		}

		if (doc != null) {
			return doc;
		}

		for (Class<?> iface : cls.getInterfaces()) {
			doc = this.getMethodDocRecurse(iface.getName(), name, desc);
			if (doc != null) {
				return doc;
			}
		}

		return null;
	}

}