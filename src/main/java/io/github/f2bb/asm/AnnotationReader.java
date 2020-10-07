package io.github.f2bb.asm;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class AnnotationReader<T extends Annotation> {
	private final T annotation;
	private final Class<T> type;

	public static void visit(Annotation annotation, MethodVisitor visitor) {
		new AnnotationReader<>(annotation).accept(visitor.visitAnnotation(Type.getDescriptor(annotation.getClass()), true));
	}

	public static void visit(Annotation annotation, FieldVisitor visitor) {
		new AnnotationReader<>(annotation).accept(visitor.visitAnnotation(Type.getDescriptor(annotation.getClass()), true));
	}

	public AnnotationReader(T annotation) {
		this.annotation = annotation;
		this.type = (Class<T>) annotation.getClass();
	}

	public void accept(AnnotationVisitor visitor) {
		try {
			for (Method method : this.type.getDeclaredMethods()) {
				Object value = method.invoke(this.annotation);
				this.accept(method.getName(), value, visitor);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void accept(String name, Object value, AnnotationVisitor visitor) {
		if(value instanceof Enum) {
			Enum e = (Enum) value;
			visitor.visitEnum(name, Type.getDescriptor(e.getDeclaringClass()), e.name());
		} else if(value instanceof Annotation) {
			new AnnotationReader<>((Annotation) value).accept(visitor.visitAnnotation(name, Type.getDescriptor(value.getClass())));
		} else if(value instanceof Class) {
			visitor.visit(name, Type.getType((Class<?>) value));
		} else if(value instanceof Object[]) {
			AnnotationVisitor visit = visitor.visitArray(name);
			if(visit != null) {
				for (Object o : ((Object[]) value)) {
					this.accept(name, o, visit);
				}
			}
		} else {
			visitor.visit(name, value);
		}
	}
}
