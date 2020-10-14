package io.github.f2bb.abstracter.func.abstracting;

import static io.github.f2bb.old.util.AbstracterUtil.map;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.RETURN;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import io.github.f2bb.abstracter.Abstracter;
import io.github.f2bb.abstracter.ex.ImplementationHiddenException;
import io.github.f2bb.abstracter.impl.AsmAbstracter;
import io.github.f2bb.abstracter.impl.JavaAbstracter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class ConstructorAbstraction {
	public static void abstractInterfaceCtorJava(TypeSpec.Builder builder, Class<?> cls, Constructor<?> ctor) {
		MethodSpec.Builder method = MethodSpec.methodBuilder("newInstance");
		method.addModifiers(Modifier.STATIC);
		method.addModifiers(Modifier.PUBLIC);
		ParameterizedTypeName name = ParameterizedTypeName.get((ClassName) JavaAbstracter.toTypeName(cls),
				map(cls.getTypeParameters(), JavaAbstracter::toTypeName, TypeName[]::new));
		method.returns(name);
		for (TypeVariable<? extends Class<?>> parameter : cls.getTypeParameters()) {
			method.addTypeVariable((TypeVariableName) JavaAbstracter.toTypeName(parameter));
		}

		for (Parameter parameter : ctor.getParameters()) {
			method.addParameter(JavaAbstracter.toTypeName(parameter.getType()), parameter.getName());
		}

		method.addStatement("throw $T.create()", ImplementationHiddenException.class);
		builder.addMethod(method.build());
	}

	public static void abstractBaseCtorJava(TypeSpec.Builder builder, Class<?> cls, Constructor<?> ctor) {
		MethodSpec.Builder method = MethodSpec.constructorBuilder();
		List<String> params = new ArrayList<>();
		for (Parameter parameter : ctor.getParameters()) {
			String name = parameter.getName();
			params.add(name);
			method.addParameter(JavaAbstracter.toTypeName(parameter.getType()), name);
		}

		for (TypeVariable<? extends Class<?>> parameter : cls.getTypeParameters()) {
			method.addTypeVariable((TypeVariableName) JavaAbstracter.toTypeName(parameter));
		}

		method.addStatement("super($L)", String.join(",", params));
		builder.addMethod(method.build());
	}

	public static void abstractInterfaceCtorAsm(ClassNode node, Class<?> cls, Constructor<?> ctor, boolean impl) {
		MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC,
				"newInstance",
				"()" + Type.getDescriptor(cls),
				null,
				null);
		if (impl) {
			String internal = Type.getInternalName(cls);
			method.visitTypeInsn(NEW, internal);
			method.visitInsn(DUP);
			Type[] types = Type.getType(ctor).getArgumentTypes();
			for (int i = 0; i < types.length; i++) {
				Type type = types[i];
				method.visitVarInsn(type.getOpcode(ILOAD), i);
			}
			method.visitMethodInsn(INVOKESPECIAL,
					internal,
					"<init>",
					Abstracter.REMAPPER.mapSignature(Type.getConstructorDescriptor(ctor), false),
					false);
			method.visitInsn(ARETURN);
		} else {
			AsmAbstracter.visitStub(method);
		}
		node.methods.add(method);
	}

	public static void abstractBaseCtorAsm(ClassNode node, Class<?> cls, Constructor<?> ctor, boolean impl) {
		String desc = Type.getConstructorDescriptor(ctor);
		MethodNode method = new MethodNode(ctor.getModifiers(), "<init>", Abstracter.REMAPPER.mapSignature(desc, false), null, null);
		if(impl) {
			Type[] types = Type.getType(ctor).getArgumentTypes();
			for (int i = 0; i < types.length; i++) {
				method.visitVarInsn(types[i].getOpcode(ILOAD), i);
			}
			method.visitMethodInsn(INVOKESPECIAL, node.superName, "<init>", desc, false);
			method.visitInsn(RETURN);
		} else {
			AsmAbstracter.visitStub(method);
		}
		node.methods.add(method);
	}
}
