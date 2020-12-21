package io.github.astrarre.abstracter.func.post;

import static java.lang.reflect.Modifier.STATIC;
import static java.lang.reflect.Modifier.isStatic;

import java.io.Serializable;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import com.google.common.reflect.TypeToken;
import io.github.astrarre.abstracter.Access;
import io.github.astrarre.abstracter.util.asm.InvokeUtil;
import io.github.astrarre.abstracter.util.ArrayUtil;
import io.github.astrarre.abstracter.util.reflect.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

@SuppressWarnings ("UnstableApiUsage")
public class ExtensionMethodPostProcessor implements PostProcessor {
	private final Method method;

	public ExtensionMethodPostProcessor(Method method) {this.method = method;}

	@Override
	public void process(Class<?> cls, ClassNode node, boolean impl) {
		Access annotation = this.method.getAnnotation(Access.class);
		int access = annotation != null ? annotation.value() : this.method.getModifiers() & ~STATIC;
		TypeToken<?>[] parameters = ArrayUtil.map(this.method.getGenericParameterTypes(), TypeToken::of, TypeToken[]::new);

		if(!isStatic(access)) {
			// first parameter is 'this'
			parameters = Arrays.copyOfRange(parameters, 1, parameters.length);
		}

		TypeToken<?> returnType = TypeToken.of(this.method.getGenericReturnType());
		String name = this.method.getName();
		String desc = TypeUtil.methodDescriptor(parameters, returnType);
		String sign = TypeUtil.methodSignature(this.method.getTypeParameters(), parameters, returnType);

		MethodNode method = new MethodNode(access, name, desc, sign, null);

		if(!Modifier.isAbstract(access)) {
			if (impl) {
				String original = Type.getMethodDescriptor(this.method);
				Type type = Type.getMethodType(original);
				Type[] types = type.getArgumentTypes();
				for (int i = 0, length = types.length; i < length; i++) {
					Type arg = types[i];
					method.visitVarInsn(arg.getOpcode(Opcodes.ILOAD), i);
				}

				method.visitMethodInsn(Opcodes.INVOKESTATIC,
						Type.getInternalName(this.method.getDeclaringClass()),
						name,
						original,
						false);
				method.visitInsn(type.getReturnType().getOpcode(Opcodes.IRETURN));
			} else {
				InvokeUtil.visitStub(method);
			}
		}

		node.methods.add(method);
	}

	// warning: cursed code from stack overflow : this is for convenience
	// https://stackoverflow.com/questions/31178103/how-can-i-find-the-target-of-a-java8-method-reference
	public static Method reverseReference(Serializable lambda) {
		SerializedLambda ser = getLambda(lambda);
		if(ser.getImplMethodKind() != MethodHandleInfo.REF_invokeStatic) {
			throw new IllegalArgumentException("Extension methods must be static!");
		}
		String owner = ser.getImplClass();
		String name = ser.getImplMethodName();
		String desc = ser.getImplMethodSignature();
		try {
			Class<?> cls = lambda.getClass().getClassLoader().loadClass(owner.replace('/', '.'));
			for (Method method : cls.getDeclaredMethods()) {
				if(name.equals(method.getName()) && desc.equals(Type.getMethodDescriptor(method))) {
					return method;
				}
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		throw new IllegalArgumentException("Unable to find method " + owner + ";" + name + ";" + desc);
	}

	private static SerializedLambda getLambda(Serializable lambda) {
		for (Class<?> cl = lambda.getClass(); cl != null; cl = cl.getSuperclass()) {
			try {
				Method m = cl.getDeclaredMethod("writeReplace");
				m.setAccessible(true);
				Object replacement = m.invoke(lambda);
				if (!(replacement instanceof SerializedLambda)) {
					break; // custom interface implementation
				}
				return (SerializedLambda) replacement;
			} catch (ReflectiveOperationException e) {
				// do nothing
			}
		}

		throw new IllegalArgumentException("lambda was not a method reference!");
	}
}
