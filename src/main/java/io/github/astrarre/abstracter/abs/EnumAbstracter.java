package io.github.astrarre.abstracter.abs;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;

import io.github.astrarre.abstracter.AbstracterConfig;
import io.github.astrarre.abstracter.func.elements.ConstructorSupplier;
import io.github.astrarre.abstracter.func.elements.FieldSupplier;
import io.github.astrarre.abstracter.func.elements.MethodSupplier;
import io.github.astrarre.abstracter.func.inheritance.InterfaceFunction;
import io.github.astrarre.abstracter.func.inheritance.SuperFunction;
import io.github.astrarre.abstracter.util.asm.FieldUtil;
import io.github.astrarre.abstracter.util.asm.MethodUtil;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class EnumAbstracter extends InterfaceAbstracter {
	public EnumAbstracter(Class<?> cls) {
		this(cls,
				getName(cls, "", 0),
				InterfaceFunction.INTERFACE_DEFAULT,
				ConstructorSupplier.INTERFACE_DEFAULT,
				FieldSupplier.INTERFACE_DEFAULT,
				MethodSupplier.INTERFACE_DEFAULT);
	}

	public EnumAbstracter(Class<?> cls,
			String name,
			InterfaceFunction interfaces,
			ConstructorSupplier supplier,
			FieldSupplier fieldSupplier,
			MethodSupplier methodSupplier) {
		super(cls, name, interfaces, supplier, fieldSupplier, methodSupplier);
		// minecraft -> api
		AbstracterConfig.TRANSLATION.put(Type.getInternalName(cls), (currentType, desiredType, visitor) -> {
			visitor.visitMethodInsn(INVOKEVIRTUAL, currentType, "ordinal", "()I");
			visitor.visitFieldInsn(GETSTATIC, this.name, "VALUES_ASTRARRE_ABS_INTERNAL", "[L" + desiredType + ";");
			visitor.visitInsn(SWAP);
			visitor.visitInsn(AALOAD);
		});
		// api -> minecraft
		AbstracterConfig.TRANSLATION.put(this.name, (currentType, desiredType, visitor) -> {
			visitor.visitMethodInsn(INVOKEVIRTUAL, currentType, "ordinal", "()I");
			visitor.visitFieldInsn(GETSTATIC, this.name, "VALUES_ASTRARRE_INT_INTERNAL", "[L" + desiredType + ";");
			visitor.visitInsn(SWAP);
			visitor.visitInsn(AALOAD);
		});
		this.superClass((a, b) -> Enum.class);
	}

	public EnumAbstracter(Class<?> cls, int version) {
		this(cls, getName(cls, "", version));
	}

	public EnumAbstracter(Class<?> cls, String name) {
		this(cls,
				name,
				InterfaceFunction.INTERFACE_DEFAULT,
				ConstructorSupplier.INTERFACE_DEFAULT,
				FieldSupplier.INTERFACE_DEFAULT,
				MethodSupplier.INTERFACE_DEFAULT);
	}

	@Override
	public int getAccess(int modifiers) {
		return modifiers;
	}

	@Override
	public void abstractField(ClassNode node, Field field, boolean impl) {
		int access = field.getModifiers();
		if ((access & ACC_ENUM) != 0) {
			node.visitField(ACC_PUBLIC | ACC_FINAL | ACC_ENUM | ACC_STATIC, field.getName(), 'L' + this.name + ';', null, null);
		} else {
			if (!Modifier.isFinal(access)) {
				MethodNode setter = FieldUtil.createSetter(this.name, this.cls, field, impl, true);
				if (!MethodUtil.conflicts(setter.name, setter.desc, node)) {
					setter.access &= ~ACC_FINAL;
					node.methods.add(setter);
				}
			}

			MethodNode getter = FieldUtil.createGetter(this.name, this.cls, field, impl, true);
			if (!MethodUtil.conflicts(getter.name, getter.desc, node)) {
				getter.access &= ~ACC_FINAL;
				node.methods.add(getter);
			}
		}
	}

	@Override
	protected void postProcess(ClassNode node, boolean impl) {
		super.postProcess(node, impl);
		if(impl) {
			MethodVisitor constructor = node.visitMethod(0, "<init>", "(Ljava/lang/String;I)V", null, null);
			constructor.visitVarInsn(ALOAD, 0);
			constructor.visitVarInsn(ALOAD, 1);
			constructor.visitVarInsn(ILOAD, 2);
			constructor.visitMethodInsn(INVOKESPECIAL, "java/lang/Enum", "<init>", "(Ljava/lang/String;I)V");
			constructor.visitInsn(RETURN);

			String target = Type.getInternalName(this.cls);
			MethodNode init = MethodUtil.findOrCreateMethod(ACC_STATIC, node, "<clinit>", "()V");
			InstructionAdapter adapter = new InstructionAdapter(init);

			List<FieldNode> fields = node.fields;
			for (int i = 0; i < fields.size(); i++) {
				FieldNode field = fields.get(i);
				init.visitTypeInsn(NEW, node.name);
				init.visitInsn(DUP);
				init.visitLdcInsn(field.name);
				adapter.iconst(i);
				init.visitMethodInsn(INVOKESPECIAL, node.name, "<init>", "(Ljava/lang/String;I)V");
				init.visitFieldInsn(PUTSTATIC, node.name, field.name, field.desc);
			}

			// create 'values' array
			init.visitIntInsn(BIPUSH, node.fields.size());
			init.visitTypeInsn(ANEWARRAY, node.name);
			for (int i = 0; i < fields.size(); i++) {
				FieldNode field = fields.get(i);
				init.visitInsn(DUP);
				adapter.iconst(i);
				init.visitFieldInsn(GETSTATIC, node.name, field.name, field.desc);
				init.visitInsn(AASTORE);
			}
			init.visitFieldInsn(PUTSTATIC, node.name, "VALUES_ASTRARRE_ABS_INTERNAL", "[L" + node.name + ';');

			// create 'values' array of the target class
			init.visitMethodInsn(INVOKESTATIC, target, "values", "()[L" + target + ';');
			init.visitFieldInsn(PUTSTATIC, node.name, "VALUES_ASTRARRE_INT_INTERNAL", "[L" + target + ';');

			// values array
			node.visitField(ACC_PUBLIC | ACC_STATIC, "VALUES_ASTRARRE_ABS_INTERNAL", "[L" + node.name + ';', null, null);
			node.visitField(ACC_PUBLIC | ACC_STATIC, "VALUES_ASTRARRE_INT_INTERNAL", "[L" + target + ';', null, null);

			init.visitInsn(RETURN);
		}
	}
}
