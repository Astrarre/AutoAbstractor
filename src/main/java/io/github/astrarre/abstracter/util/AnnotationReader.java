package io.github.astrarre.abstracter.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;

public class AnnotationReader {
	public static AnnotationNode accept(Annotation annotation) {
		try {
			Class<?> type = annotation.annotationType();
			AnnotationNode node = new AnnotationNode(Type.getDescriptor(type));
			visit(node, annotation);
			return node;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private static void visit(AnnotationVisitor visitor, Annotation annotation)
			throws InvocationTargetException, IllegalAccessException {
		Class<?> type = annotation.annotationType();
		for (Method method : type.getDeclaredMethods()) {
			String name = method.getName();
			Object object = method.invoke(annotation);
			visit(visitor, name, object);
		}
		visitor.visitEnd();
	}

	private static void visit(AnnotationVisitor visitor, String name, Object object)
			throws InvocationTargetException, IllegalAccessException {
		Class<?> ret = object.getClass();
		if (object instanceof String || object instanceof Number || object instanceof Character || object instanceof Boolean) {
			visitor.visit(name, object);
		} else if(object instanceof Class<?>) {
			visitor.visit(name, Type.getType((Class<?>) object));
		} else if(object instanceof Enum) {
			visitor.visitEnum(name, Type.getDescriptor(ret), ((Enum) object).name());
		} else if(object instanceof Annotation) {
			Annotation anno = (Annotation) object;
			visit(visitor.visitAnnotation(name, Type.getDescriptor(anno.annotationType())), anno);
		} else if(ret.isArray()) {
			if(ret.getComponentType().isPrimitive()) {
				visitor.visit(name, object);
			} else {
				visitor.visitArray(name);
				for (Object o : (Object[]) object) {
					visit(visitor, name, o);
				}
			}
		}
	}
}
