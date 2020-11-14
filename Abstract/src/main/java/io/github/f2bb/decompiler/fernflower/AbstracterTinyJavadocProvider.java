package io.github.f2bb.decompiler.fernflower;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.f2bb.abstracter.AbstracterConfig;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.objectweb.asm.commons.Remapper;

import net.fabricmc.fernflower.api.IFabricJavadocProvider;
import net.fabricmc.mapping.tree.ClassDef;
import net.fabricmc.mapping.tree.FieldDef;
import net.fabricmc.mapping.tree.MethodDef;
import net.fabricmc.mapping.tree.ParameterDef;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;
import net.fabricmc.mappings.EntryTriple;

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
		FieldDef fieldDef = this.fields.get(new EntryTriple(REMAPPER.map(structClass.qualifiedName),
				structField.getName(),
				REMAPPER.mapDesc(structField.getDescriptor())));
		return fieldDef != null ? fieldDef.getComment() : null;
	}

	@Override
	public String getMethodDoc(StructClass structClass, StructMethod structMethod) {
		MethodDef methodDef = this.methods.get(new EntryTriple(structClass.qualifiedName,
				structMethod.getName(),
				REMAPPER.mapMethodDesc(structMethod.getDescriptor())));

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
}