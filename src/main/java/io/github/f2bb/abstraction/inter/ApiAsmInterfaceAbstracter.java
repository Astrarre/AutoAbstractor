package io.github.f2bb.abstraction.inter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.f2bb.abstraction.AbstractAbstracter;
import io.github.f2bb.classpath.AbstractorClassLoader;
import io.github.f2bb.reflect.ReifiedType;
import io.github.f2bb.util.AsmUtil;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class ApiAsmInterfaceAbstracter extends AbstractAbstracter {
	public String name;

	public ApiAsmInterfaceAbstracter(AbstractorClassLoader loader, Class<?> cls) {
		super(new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES), loader, cls);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.name = name;
		Class<?> sup = this.getValidSuper();
		ReifiedType superClass = new ReifiedType(this.reify(sup), sup);
		Set<ReifiedType> reifiedInterfaces = this.reify(this.getInterfaces(this.cls));
		// generate class signature
		signature = this.toSignature(this.cls.getTypeParameters(),
				OBJECT,
				reifiedInterfaces.stream()
						.map(ReifiedType::getType)
						.collect(Collectors.toList())) + this.prefixSign("I", superClass.raw, superClass.type);
		List<String> ifaces = reifiedInterfaces.stream()
				                      .map(ReifiedType::getRaw)
				                      .map(AbstractAbstracter::getInternalName)
				                      .collect(Collectors.toList());
		ifaces.add(this.prefix("I", superClass.raw));
		super.visit(version, access, AsmUtil.prefixName("Base", name), // name
				signature, // signature
				OBJECT, // super
				ifaces.toArray(new String[0]));
	}

	@Override
	public MethodVisitor visitMethod(int access,
			String name,
			String descriptor,
			String signature,
			String[] exceptions) {
		if ((ACC_PUBLIC & access) != 0) {
			MethodVisitor visitor = super.visitMethod(access,
					name,
					this.loader.remap(descriptor),
					this.loader.remap(signature),
					null);

			return new MethodVisitor(ASM9) {
				@Override
				public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
					return visitor.visitAnnotation(descriptor, visible);
				}

				@Override
				public void visitParameter(String name, int access) {
					visitor.visitParameter(name, access);
				}
			};
		}
		return null;
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if ((ACC_PUBLIC & access) != 0) {
			FieldVisitor get, set = null;
			if ((access & ACC_FINAL) == 0) {
				set = AsmUtil.generateSetter(super::visitMethod,
						this.name,
						access,
						name,
						this.loader.remap(descriptor),
						this.loader.remap(signature),
						true);
			}
			get = AsmUtil.generateGetter(super::visitMethod,
					this.name,
					access,
					name,
					this.loader.remap(descriptor),
					this.loader.remap(signature),
					true);
			// todo visit setter as well
			return new FieldVisitor(ASM9) {
				@Override
				public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
					return get.visitAnnotation(descriptor, visible);
				}
			};
		}
		return null;
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		super.visitInnerClass(this.prefix("I", name), this.prefix("I", name), innerName, access);
	}

	@Override
	public void visitOuterClass(String owner, String name, String descriptor) {
		super.visitOuterClass(AsmUtil.prefixName("I", owner), name, descriptor);
	}

	@Override
	public Optional<Resource> write() {
		return Optional.of(new Resource() {
			@Override
			public String getPath() {
				return AsmUtil.prefixName("I", ApiAsmInterfaceAbstracter.this.name) + ".class";
			}

			@Override
			public void write(OutputStream stream) throws IOException {
				stream.write(((ClassWriter) ApiAsmInterfaceAbstracter.this.cv).toByteArray());
			}
		});
	}
}
