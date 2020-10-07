package io.github.f2bb.abstraction.base;

import static javax.lang.model.element.Modifier.ABSTRACT;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import io.github.f2bb.abstraction.AbstractBaseAbstracter;
import io.github.f2bb.abstraction.inter.ApiAsmInterfaceAbstracter;
import io.github.f2bb.classpath.AbstractorClassLoader;
import io.github.f2bb.reflect.ReifiedType;
import io.github.f2bb.util.AsmUtil;
import io.github.f2bb.util.JavaUtil;
import io.github.f2bb.util.ReflectUtil;
import org.objectweb.asm.AnnotationVisitor;

// only call for non - inner classes
public class ApiJavaBaseAbstracter extends AbstractBaseAbstracter {
	private TypeSpec.Builder builder;

	public ApiJavaBaseAbstracter(AbstractorClassLoader loader, Class<?> cls) {
		super(null, loader, cls);
		// todo inner classes
	}

	@Override
	public void visit(int version, int access, String name, TypeVariable<?>[] typeVariables, ReifiedType superClass, Collection<ReifiedType> interfaces) {
		// interface builder
		this.builder = (access & ACC_INTERFACE) == 0 ? TypeSpec.classBuilder(AsmUtil.prefixName("Base", name)) : TypeSpec.interfaceBuilder(AsmUtil.prefixName("Base", name));
		for (TypeVariable<?> variable : typeVariables) {
			this.builder.addTypeVariable(TypeVariableName.get(variable));
		}
		this.builder.superclass(superClass.type);
		for (ReifiedType anInterface : interfaces) {
			// todo add "This" interface
			this.builder.addSuperinterface(anInterface.type);
		}

		if ((access & ACC_ABSTRACT) != 0) {
			this.builder.addModifiers(ABSTRACT);
		}
	}

	@Override
	public void visit(Constructor constructor, TypeVariable<?>[] variable, Type[] parameters) {
		MethodSpec.Builder builder = MethodSpec.constructorBuilder();
		List<String> params = new ArrayList<>();
		for (Parameter parameter : constructor.getParameters()) {
			String s = parameter.getName();
			params.add(s);
			builder.addParameter(parameter.getParameterizedType(), s);
		}
		builder.addModifiers(ReflectUtil.getModifiers(constructor.getModifiers()));
		builder.addStatement(String.format("super(%s);", String.join(",", params)));
		for (Annotation annotation : constructor.getAnnotations()) {
			builder.addAnnotation(AnnotationSpec.get(annotation));
		}
		this.builder.addMethod(builder.build());
	}

	@Override
	public void visit(Method method, TypeVariable<?>[] variable, Type[] parameters, Type returnType) {
		MethodSpec.Builder builder = JavaUtil.generateEmpty(method.getName(), method.getParameters(), parameters, returnType, method.getDeclaredAnnotations());
		for (TypeVariable<?> typeVariable : variable) {
			builder.addTypeVariable(TypeVariableName.get(typeVariable));
		}
		this.builder.addMethod(builder.build());
	}

	@Override
	public void visit(Field field, Type type) {
		int access = field.getModifiers();
		Class<?> raw = this.raw(type);
		if (raw != field.getType() || Modifier.isStatic(access) || this.loader.isMinecraft(raw)) {
			// getter
			this.builder.addMethod(this.visitEnd(JavaUtil.generateEmpty(AsmUtil.getEtterName("get", org.objectweb.asm.Type.getDescriptor(field.getType()), field.getName()),
			                                                            new String[] {field.getName()},
			                                                            new Type[0],
			                                                            type,
			                                                            field.getDeclaredAnnotations()), field).build());
			if (!Modifier.isFinal(access)) {
				// setter
				this.builder.addMethod(this.visitEnd(JavaUtil.generateEmpty(AsmUtil.getEtterName("get", org.objectweb.asm.Type.getDescriptor(field.getType()), field.getName()),
				                                                            new String[] {field.getName()},
				                                                            new Type[0],
				                                                            type,
				                                                            field.getDeclaredAnnotations()), field).build());
			}
		} else {
			// did you know? fields have virtual lookups!
			// visit raw field
			FieldSpec.Builder builder = FieldSpec.builder(type, field.getName());
			ReflectUtil.getModifiers(field.getModifiers()).forEach(builder::addModifiers);
			for (Annotation annotation : field.getDeclaredAnnotations()) {
				builder.addAnnotation(AnnotationSpec.get(annotation));
			}
			this.builder.addField(builder.build());
		}
	}

	protected MethodSpec.Builder visitEnd(MethodSpec.Builder builder, Field field) {
		for (Annotation annotation : field.getAnnotations()) {
			builder.addAnnotation(AnnotationSpec.get(annotation));
		}
		return builder;
	}

	// todo we need to override all the class visitor methods, including like annotations n stuff


	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		// todo annotation spec
		return null;
	}

	@Override
	public Optional<Resource> write() {
		return Optional.of(new Resource() {
			@Override
			public String getPath() {
				return AsmUtil.prefixName("Base", ApiJavaBaseAbstracter.this.name) + ".java";
			}

			@Override
			public void write(OutputStream stream) throws IOException {
				stream.write(ApiJavaBaseAbstracter.this.builder.build().toString().getBytes(StandardCharsets.UTF_8));
			}
		});
	}
}
