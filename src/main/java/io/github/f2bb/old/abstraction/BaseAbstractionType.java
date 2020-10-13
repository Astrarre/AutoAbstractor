package io.github.f2bb.old.abstraction;

import static java.lang.reflect.Modifier.isAbstract;
import static org.objectweb.asm.Opcodes.ACC_BRIDGE;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Collections2;
import com.google.common.reflect.TypeToken;
import io.github.f2bb.abstracter.Abstracter;
import io.github.f2bb.abstracter.util.RawClassType;

@SuppressWarnings ("UnstableApiUsage")
public class BaseAbstractionType implements AbstractionType {
	public static final int METHOD_FILTER = ACC_BRIDGE | ACC_SYNTHETIC | ACC_PRIVATE;
	protected final Class<?> cls;
	protected final TypeToken<?> token;
	private final List<TypeToken<?>> interfaces;
	private final Collection<Method> methods;
	private final Collection<Field> fields;

	public BaseAbstractionType(Class<?> cls) {
		this.cls = cls;
		this.token = TypeToken.of(cls);
		List<TypeToken<?>> interfaces = new ArrayList<>();
		interfaces.add(TypeToken.of(new RawClassType(Abstracter.getInterfaceName(this.cls))));
		this.visitInterfaces(this.cls, interfaces);
		this.interfaces = Collections.unmodifiableList(interfaces);
		Map<String, Method> map = new HashMap<>();
		this.walk(cls, map);
		this.methods = Collections.unmodifiableCollection(Collections2.filter(map.values(),
				i -> (i.getModifiers() & METHOD_FILTER) == 0 ||
				     // abstract method exception go brrrs
				     isAbstract(i.getModifiers())));
		List<Field> arr = new ArrayList<>();
		this.getFields(this.cls, arr);
		this.fields = Collections.unmodifiableCollection(arr);
	}

	@Override
	public TypeToken<?> getSuperClass() {
		Class<?> current = this.cls;
		while (Abstracter.isMinecraft(current)) {
			current = current.getSuperclass();
		}
		return this.token.resolveType(current);
	}

	@Override
	public List<TypeToken<?>> getInterfaces() {
		return this.interfaces;
	}

	@Override
	public Collection<Method> getMethods() {
		return this.methods;
	}

	@Override
	public Collection<Field> getFields() {
		return this.fields;
	}



	public void getFields(Class<?> cls, List<Field> fields) {
		Class<?> sup = cls.getSuperclass();
		if (Abstracter.isMinecraft(sup)) {
			this.getFields(cls.getSuperclass(), fields);
		}

		for (Field field : cls.getDeclaredFields()) {
			int access = field.getModifiers();
			// if it is an instance field, we should still abstract it even if it's public because
			// it means we can put `final` on the getter even in java 8
			// however, public static fields and methods can be handled by interface abstractions
			if(Modifier.isStatic(access) && !Modifier.isProtected(access)) {
				break;
			}
		}

		fields.addAll(Arrays.asList(cls.getDeclaredFields()));
	}
}
