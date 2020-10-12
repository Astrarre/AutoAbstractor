package io.github.f2bb.abstraction.selector;

import static java.lang.reflect.Modifier.isAbstract;
import static org.objectweb.asm.Opcodes.ACC_BRIDGE;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Collections2;
import io.github.f2bb.loader.AbstracterLoader;
import org.objectweb.asm.Type;

public class AllInheritedMethodSelector implements MethodSelector {
	public static final int METHOD_FILTER = ACC_BRIDGE | ACC_SYNTHETIC | ACC_PRIVATE;
	protected final AbstracterLoader loader;

	public AllInheritedMethodSelector(AbstracterLoader loader) {this.loader = loader;}

	@Override
	public Iterable<Method> getMethods(Class<?> cls) {
		Map<String, Method> map = new HashMap<>();
		walk(cls, map);
		return Collections2.filter(map.values(), i -> (i.getModifiers() & METHOD_FILTER) == 0 ||
		                                              // abstract method exception go brrrs
		                                              isAbstract(i.getModifiers()));
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
}
