package io.github.f2bb.old.abstraction.base;

import static io.github.f2bb.old.util.AbstracterUtil.map;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.lang.model.element.Modifier;

import com.google.common.reflect.TypeToken;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.github.f2bb.abstracter.ex.DoNotOverride;
import io.github.f2bb.abstracter.ex.ImplementationHiddenException;
import io.github.f2bb.old.loader.AbstracterLoader;
import io.github.f2bb.old.util.AbstracterUtil;
import io.github.f2bb.old.util.AsmUtil;

public class JavaBaseAbstracter extends AbstractBaseAbstracter {
	public JavaBaseAbstracter(AbstracterLoader loader, Class<?> toAbstract) {
		super(loader, toAbstract);
	}

	private TypeSpec.Builder builder;
	@Override
	public void write(ZipOutputStream out) throws IOException {
		this.builder = TypeSpec.classBuilder("Base" + this.cls.getSimpleName());
		this.builder.addModifiers(AbstracterUtil.getModifiers(this.cls.getModifiers()).toArray(new Modifier[0]));
		Class<?> sup = this.findSuper();
		Class<?>[] interfaces = this.getInterfaces();
		this.builder.superclass(sup);
		for (Class<?> anInterface : interfaces) {
			this.builder.addSuperinterface(anInterface);
		}
		this.builder.addSuperinterface(this.toTypeName(this.cls));
		// todo fucking inner classes
		super.write(out);
		out.putNextEntry(new ZipEntry(this.abstractionType.getBaseAbstractedName(this.cls) + ".java"));
		OutputStreamWriter writer = new OutputStreamWriter(out);
		writer.write(this.builder.build().toString());
		writer.flush();
		out.closeEntry();
	}

	// doesn't exist in api
	@Override
	public void visitBridge(Method method, String target) {}

	@Override
	public String visitBridged(Method method) {
		Type[] params = map(method.getGenericParameterTypes(), this::resolve, Type[]::new);
		Type returnType = this.resolve(method.getGenericReturnType());
		MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getName());
		Parameter[] parameters = method.getParameters();
		for (int i = 0; i < parameters.length; i++) {
			builder.addParameter(this.toTypeName(params[i]), parameters[i].getName());
		}
		builder.returns(this.toTypeName(returnType));
		builder.addStatement("throw $T.create()", ImplementationHiddenException.class);
		this.builder.addMethod(builder.build());
		return null;
	}

	@Override
	public void visitFieldGetter(TypeToken<?> token, Field field) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder(AsmUtil.getEtterName("get", field.getType(), field.getName()));
		builder.returns(this.toTypeName(token.getType()));
		builder.addStatement("throw $T.create()", ImplementationHiddenException.class);
		builder.addAnnotation(AnnotationSpec.builder(DoNotOverride.class).build());
		this.builder.addMethod(builder.build());
	}

	@Override
	public void visitFieldSetter(TypeToken<?> token, Field field) {
		MethodSpec.Builder builder = MethodSpec.methodBuilder(AsmUtil.getEtterName("set", field.getType(), field.getName()));
		builder.addParameter(this.toTypeName(field.getGenericType()), field.getName());
		builder.addStatement("throw $T.create()", ImplementationHiddenException.class);
		builder.addAnnotation(AnnotationSpec.builder(DoNotOverride.class).build());
		this.builder.addMethod(builder.build());
	}

	@Override
	public void visitEmptyField(TypeToken<?> token, Field field) {
		List<Modifier> modifiers = AbstracterUtil.getModifiers(field.getModifiers());
		modifiers.add(Modifier.FINAL);
		FieldSpec.Builder builder = FieldSpec.builder(this.toTypeName(field.getGenericType()), field.getName(),
				modifiers.toArray(new Modifier[0]));
		builder.initializer("$T.instance()", ImplementationHiddenException.class);
		// todo initializer or something
		this.builder.addField(builder.build());
	}
}
