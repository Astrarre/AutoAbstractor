package io.github.f2bb.old.abstraction.iface;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.zip.ZipOutputStream;

import com.google.common.reflect.TypeToken;
import io.github.f2bb.old.abstraction.AbstractAbstracter;
import io.github.f2bb.old.loader.AbstracterLoader;
import io.github.f2bb.old.util.AbstracterUtil;

@SuppressWarnings ("UnstableApiUsage")
public abstract class AbstractInterfaceAbstracter extends AbstractAbstracter {
	protected AbstractInterfaceAbstracter(AbstracterLoader loader, Class<?> cls) {
		super(loader, cls);
	}

	@Override
	public void write(ZipOutputStream out) throws IOException {
		for (Field field : this.cls.getFields()) {
			Class<?> dec = field.getDeclaringClass();
			// only abstract if we will abstract the field's type
			if ((dec == this.cls || !this.abstractionType.isValid(dec)) && !this.abstractionType.containsInvalidClasses(AbstracterUtil.getSign(
					field))) {
				java.lang.reflect.Type type = field.getGenericType();
				TypeToken<?> token = this.resolved(type);
				if (!Modifier.isFinal(field.getModifiers())) {
					this.visitSetter(field, token);
				}
				this.visitGetter(field, token);
			}
		}

		for (Method method : this.cls.getMethods()) {
			Class<?> dec = method.getDeclaringClass();
			if ((dec == this.cls || !this.abstractionType.isValid(dec)) && !this.abstractionType.containsInvalidClasses(AbstracterUtil.getSign(
					method))) {
				this.visitDelegate(method);
			}
		}
	}

	public abstract void visitGetter(Field field, TypeToken<?> type);

	public abstract void visitSetter(Field field, TypeToken<?> type);

	public abstract void visitDelegate(Method method);
}
