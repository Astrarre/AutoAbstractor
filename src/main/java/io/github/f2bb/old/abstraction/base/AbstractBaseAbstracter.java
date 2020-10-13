package io.github.f2bb.old.abstraction.base;

import static io.github.f2bb.old.util.AbstracterUtil.map;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isProtected;
import static java.lang.reflect.Modifier.isStatic;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.zip.ZipOutputStream;

import com.google.common.reflect.TypeToken;
import io.github.f2bb.old.abstraction.AbstractAbstracter;
import io.github.f2bb.old.abstraction.BaseAbstractionType;
import io.github.f2bb.abstracter.Abstracter;

@SuppressWarnings ("UnstableApiUsage")
public abstract class AbstractBaseAbstracter extends AbstractAbstracter {
	protected AbstractBaseAbstracter(Class<?> toAbstract) {
		super(new BaseAbstractionType(toAbstract), toAbstract);
	}

	@Override
	public void write(ZipOutputStream out) throws IOException {
		for (Field field : this.abstractionType.getFields()) {
			java.lang.reflect.Type type = field.getGenericType();
			TypeToken<?> token = this.resolved(type);
			String signature = this.toSignature(field.getGenericType(), false);
			// only abstract if we will abstract the field's type
			if (Abstracter.isValid(signature)) {
				this.visitField(field, token);
			}
		}

		for (Method method : this.abstractionType.getMethods()) {
			int access = method.getModifiers();
			// if instance, abstract
			if (!isStatic(access) ||
			    // if the static method is protected, and the class that declared it is not abstracted, then we need
			    // to expose it ourselves
			    isProtected(access) && !Abstracter.isBaseAbstracted(method.getDeclaringClass())) {
				TypeToken<?>[] params = map(method.getGenericParameterTypes(), this::resolved, TypeToken[]::new);
				TypeToken<?> returnType = this.resolved(method.getGenericReturnType());
				String sign = this.methodSignature(method.getTypeParameters(), params, returnType, false);
				if (Abstracter.isValid(sign)) {
					this.visitMethod(method);
				} else if (isAbstract(access)) {
					// todo silence if an extension method fills it in

				}
			}
		}
	}

	public void visitField(Field field, TypeToken<?> resolved) {
		int access = field.getModifiers();
		if (!Abstracter.isMinecraft(resolved.getRawType()) && !isStatic(access)) {
			this.visitEmptyField(resolved, field);
		} else {
			if (!isFinal(access)) {
				this.visitFieldSetter(resolved, field);
			}
			this.visitFieldGetter(resolved, field);
		}
	}

	public void visitMethod(Method method) {
		this.visitBridge(method, this.visitBridged(method));
	}

	public abstract void visitBridge(Method method, String target);

	// the api facing method

	/**
	 * @return the real descriptor of the method (post reification)
	 */
	public abstract String visitBridged(Method method);

	/**
	 * visit the getter for the field
	 */
	public abstract void visitFieldGetter(TypeToken<?> token, Field field);

	/**
	 * visit the setter for the field
	 */
	public abstract void visitFieldSetter(TypeToken<?> token, Field field);

	/**
	 * a field that does not need abstracting because java has virtual field lookups :tada:
	 */
	public abstract void visitEmptyField(TypeToken<?> token, Field field);
}
