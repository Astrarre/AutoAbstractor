package io.github.f2bb.old.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.logging.Logger;

import com.google.common.reflect.TypeToken;
import io.github.f2bb.abstracter.ex.DoNotOverride;
import io.github.f2bb.abstracter.ex.ImplementationHiddenException;
import io.github.f2bb.abstracter.impl.AsmAbstracter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

@SuppressWarnings ("UnstableApiUsage")
public class AsmUtil implements Opcodes {

	public static final String OBJECT_NAME = Type.getInternalName(Object.class);
	public static final String OBJECT_DESC = Type.getDescriptor(Object.class);
	private static final Logger LOGGER = Logger.getLogger("AsmUtil");


}