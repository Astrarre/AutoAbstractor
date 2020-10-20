package io.github.f2bb.abstracter.func.abstracting;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

import com.google.common.reflect.TypeToken;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.github.f2bb.DoNotOverride;
import io.github.f2bb.ImplementationHiddenException;
import io.github.f2bb.abstracter.func.map.TypeMappingFunction;
import io.github.f2bb.abstracter.impl.JavaUtil;
import io.github.f2bb.abstracter.util.asm.InvokeUtil;
import io.github.f2bb.abstracter.util.asm.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

@SuppressWarnings ("UnstableApiUsage")
public class FieldAbstraction implements Opcodes {
	public static final String DO_NOT_OVERRIDE = Type.getDescriptor(DoNotOverride.class);

	public static FieldAbstracter<TypeSpec.Builder> getJavaGetter(boolean iface) {
		return (h, c, f) -> {
			MethodSpec.Builder builder = MethodSpec.methodBuilder(FieldAbstraction.getEtterName("get",
					f.getType(),
					f.getName()));
			Set<javax.lang.model.element.Modifier> modifiers = JavaUtil.getModifiers(f.getModifiers());
			if (iface) {
				modifiers.remove(javax.lang.model.element.Modifier.FINAL);
				if (!modifiers.contains(javax.lang.model.element.Modifier.STATIC)) {
					modifiers.add(javax.lang.model.element.Modifier.DEFAULT);
					builder.addAnnotation(AnnotationSpec.builder(DoNotOverride.class).build());
				}
			} else {
				modifiers.add(javax.lang.model.element.Modifier.FINAL);
			}
			builder.addModifiers(modifiers);

			builder.returns(JavaUtil.toTypeName(TypeMappingFunction.reify(c, f.getGenericType())));
			builder.addStatement("throw $T.create()", ImplementationHiddenException.class);
			h.addMethod(builder.build());
		};
	}

	public static FieldAbstracter<TypeSpec.Builder> getJavaSetter(boolean iface) {
		return (h, c, f) -> {
			MethodSpec.Builder builder = MethodSpec.methodBuilder(FieldAbstraction.getEtterName("set",
					f.getType(),
					f.getName()));
			Set<javax.lang.model.element.Modifier> modifiers = JavaUtil.getModifiers(f.getModifiers());
			if (iface) {
				modifiers.remove(javax.lang.model.element.Modifier.FINAL);
				modifiers.add(javax.lang.model.element.Modifier.DEFAULT);
			} else {
				modifiers.add(javax.lang.model.element.Modifier.FINAL);
			}
			builder.addModifiers(modifiers);

			builder.addParameter(JavaUtil.toTypeName(TypeMappingFunction.reify(c, f.getGenericType())), f.getName());
			builder.addStatement("throw $T.create()", ImplementationHiddenException.class);
			builder.addAnnotation(AnnotationSpec.builder(DoNotOverride.class).build());
			h.addMethod(builder.build());
		};
	}

	public static MethodNode generateGetter(Class<?> cls, Field field, boolean impl, boolean iface) {
		int access = field.getModifiers();
		String owner = Type.getInternalName(field.getType());
		TypeToken<?> token = TypeToken.of(cls);
		String descriptor = Type.getDescriptor(token.getRawType());
		String name = field.getName();
		String signature = TypeUtil.toSignature(token.getType());
		if(iface) {
			access &= ~ACC_FINAL;
		} else {
			access |= ACC_FINAL;
		}
		MethodNode node = new MethodNode(access,
				getEtterName("get", descriptor, name),
				"()" + descriptor,
				signature.equals(descriptor) ? null : "()" + signature,
				null);
		if (impl) {
			if (Modifier.isStatic(access)) {
				node.visitFieldInsn(GETSTATIC, owner, name, descriptor);
			} else {
				node.visitVarInsn(ALOAD, 0);
				node.visitFieldInsn(GETFIELD, owner, name, descriptor);
			}
			node.visitInsn(Type.getType(descriptor).getOpcode(IRETURN));
		} else {
			InvokeUtil.visitStub(node);
		}
		node.visitAnnotation(DO_NOT_OVERRIDE, true);
		return node;
	}

	public static String getEtterName(String prefix, Class<?> desc, String name) {
		return prefix + name(Type.getDescriptor(desc)) + Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	public static String getEtterName(String prefix, String desc, String name) {
		return prefix + name(desc) + Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	public static MethodNode generateSetter(Class<?> cls, Field field, boolean impl, boolean iface) {
		int access = field.getModifiers();
		String owner = Type.getInternalName(field.getType());
		TypeToken<?> token = TypeToken.of(cls);
		String descriptor = Type.getDescriptor(token.getRawType());
		String name = field.getName();
		String signature = TypeUtil.toSignature(token.getType());
		if(iface) {
			access &= ~ACC_FINAL;
		} else {
			access |= ACC_FINAL;
		}
		MethodNode node = new MethodNode(access,
				getEtterName("set", descriptor, name),
				"(" + descriptor + ")V",
				signature.equals(descriptor) ? null : "(" + signature + ")V",
				null);
		Type type = Type.getType(descriptor);
		if (impl) {
			if (Modifier.isStatic(access)) {
				node.visitVarInsn(type.getOpcode(ILOAD), 0);
				node.visitFieldInsn(PUTSTATIC, owner, name, descriptor);
			} else {
				node.visitVarInsn(ALOAD, 0);
				node.visitVarInsn(type.getOpcode(ILOAD), 1);
				if(!Type.getDescriptor(field.getType()).equals(descriptor)) {
					node.visitTypeInsn(CHECKCAST, Type.getInternalName(field.getType()));
				}
				node.visitFieldInsn(PUTFIELD, owner, name, descriptor);
			}
		} else {
			InvokeUtil.visitStub(node);
		}
		node.visitInsn(RETURN);
		node.visitParameter(name, ACC_FINAL);
		node.visitAnnotation(DO_NOT_OVERRIDE, true);
		return node;
	}

	private static String name(String desc) {
		Type type = Type.getType(desc);
		switch (type.getSort()) {
		case Type.ARRAY:
			return "Arr";
		case Type.OBJECT:
			return "Obj";
		case Type.BYTE:
			return "Byte";
		case Type.BOOLEAN:
			return "Bool";
		case Type.SHORT:
			return "Short";
		case Type.CHAR:
			return "Char";
		case Type.INT:
			return "Int";
		case Type.FLOAT:
			return "Float";
		case Type.LONG:
			return "Long";
		case Type.DOUBLE:
			return "Double";
		}
		throw new IllegalArgumentException(desc);
	}
}
