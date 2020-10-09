package net.devtech.test.signatures;

import java.util.List;

import io.github.f2bb.utils.types.Arguments;
import org.objectweb.asm.signature.SignatureReader;

public class B {
	public static void main(String[] args) throws ClassNotFoundException {
		Arguments arguments = new Arguments();
		SignatureReader reader = new SignatureReader("LTest$Yeef<LBruh<LTest;>;LE;>.Test2.Concern<LInteger;LString<LBruh;>;>;");
		reader.accept(arguments);
		for (List<String> argument : arguments.arguments) {
			System.out.println(argument);
		}
		System.out.println(arguments.desc);
	}
}
