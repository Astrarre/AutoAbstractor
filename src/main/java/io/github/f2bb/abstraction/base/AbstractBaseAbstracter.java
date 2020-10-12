package io.github.f2bb.abstraction.base;

import static io.github.f2bb.util.AbstracterUtil.map;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isProtected;
import static java.lang.reflect.Modifier.isStatic;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import com.google.common.collect.Collections2;
import com.google.common.reflect.TypeToken;
import io.github.f2bb.abstraction.AbstractAbstracter;
import io.github.f2bb.loader.AbstracterLoader;
import org.objectweb.asm.Type;

// todo no raw type impl, instead just tack on the raw string at the end
@SuppressWarnings ("UnstableApiUsage")
public abstract class AbstractBaseAbstracter extends AbstractAbstracter {
	// public is handled by interface abstraction
	// static is handled individually
	// public, protected, package-protected
	public static final int METHOD_FILTER = ACC_BRIDGE | ACC_SYNTHETIC | ACC_PRIVATE;

	protected AbstractBaseAbstracter(AbstracterLoader loader, Class<?> toAbstract) {
		super(loader, toAbstract);
	}

	@Override
	public void write(ZipOutputStream out) throws IOException {
		for (Field field : this.getFields(this.cls)) {
			// todo maybe best match super? instead of just flat out not exposing the field
			// todo but then Object go brr, idk
			int access = field.getModifiers();
			// only protected fields need to be abstracted
			// public fields are handled by interface abstraction
			// private/package-private fields shouldn't be abstracted
			if (isProtected(access)) {
				// if it's an instance field, then we abstract it
				// if the class that declared a static method is not abstracted, we need to do it ourselves
				if (!isStatic(access) || !this.loader.isBaseAbstracted(field.getDeclaringClass())) {
					java.lang.reflect.Type type = field.getGenericType();
					TypeToken<?> token = this.resolved(type);
					String signature = this.toSignature(field.getGenericType(), false);
					// only abstract if we will abstract the field's type
					if (!this.loader.containsInvalidClasses(signature)) {
						this.visitField(field, token);
					}
				}
			}
		}

		// todo constructors
		for (Method method : this.getAllInheritedMethods(this.cls)) {
			int access = method.getModifiers();
			// if instance, abstract
			if (!isStatic(access) ||
			    // if the static method is protected, and the class that declared it is not abstracted, then we need
			    // to expose it ourselves
			    isProtected(access) && !this.loader.isBaseAbstracted(method.getDeclaringClass())) {
				TypeToken<?>[] params = map(method.getGenericParameterTypes(), this::resolved, TypeToken[]::new);
				TypeToken<?> returnType = this.resolved(method.getGenericReturnType());
				String sign = this.methodSignature(method.getTypeParameters(), params, returnType, false);
				if (!this.loader.containsInvalidClasses(sign)) {
					this.visitMethod(method);
				} else if (isAbstract(access)) {
					// todo silence if an extension method fills it in
					LOGGER.severe("Method " + method + " could not be abstracted because it contained a non" +
					              "-abstracted class, but it is `abstract`, if the method is not exposed, it will" +
					              " cause an abstract exception thing");
				}
			}
		}
	}

	public void visitField(Field field, TypeToken<?> resolved) {
		int access = field.getModifiers();
		if (!this.loader.isMinecraft(resolved.getRawType()) && !isStatic(access)) {
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

	public Class<?> findSuper() {
		Class<?> current = this.cls;
		while (this.loader.isMinecraft(current)) {
			current = current.getSuperclass();
		}
		return current;
	}

	/**
	 * get all non-static, non-bridge, non-synthetic inherited methods
	 */
	public Collection<Method> getAllInheritedMethods(Class<?> cls) {
		Map<String, Method> map = new HashMap<>();
		walk(cls, map);
		return Collections2.filter(map.values(), i -> (i.getModifiers() & METHOD_FILTER) == 0 ||
		                                              // abstract method exception go brrrs
		                                              isAbstract(i.getModifiers()));
	}

	/**
	 * get all non-static, non-bridge, non-synthetic inherited methods todo deal with instance inner class
	 */
	public Collection<Field> getFields(Class<?> cls) {
		List<Field> fields = new ArrayList<>();
		Class<?> sup = cls.getSuperclass();
		if (this.loader.isMinecraft(sup)) {
			fields.addAll(this.getFields(cls.getSuperclass()));
		}

		fields.addAll(Arrays.asList(cls.getDeclaredFields()));
		return fields;
	}

	public Class<?>[] getInterfaces() {
		List<Class<?>> interfaces = new ArrayList<>();
		this.visitInterfaces(this.cls, interfaces);
		return interfaces.toArray(new Class[0]);
	}

	private void walk(Class<?> cls, Map<String, Method> map) {
		if(!this.loader.isMinecraft(cls)) return;

		// inverse virtual order, interface -> super -> this
		for (Class<?> iface : cls.getInterfaces()) {
			this.walk(iface, map);
		}

		this.walk(cls.getSuperclass(), map);
		for (Method method : cls.getDeclaredMethods()) {
			String desc = Type.getMethodDescriptor(method);
			map.put(desc, method);
		}
	}

	private void visitInterfaces(Class<?> cls, List<Class<?>> classes) {
		for (Class<?> iface : cls.getInterfaces()) {
			if (this.loader.isAbstracted(iface)) {
				classes.add(iface);
			} else {
				// if the interface is not abstracted, then find all the interfaces that that interface implements
				// and check if any of those are abstracted, no interface left behind.
				this.visitInterfaces(iface, classes);
			}
		}
		Class<?> sup = cls.getSuperclass();
		if (this.loader.isMinecraft(sup) && !this.loader.isAbstracted(sup)) {
			this.visitInterfaces(sup, classes);
		}
	}
}
