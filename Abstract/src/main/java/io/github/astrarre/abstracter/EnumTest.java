package io.github.astrarre.abstracter;

public enum EnumTest {
	EEEEE(3),
	AAAAA(4);

	EnumTest(int i) {
		i *= 3;
		System.out.println(i);
	}
}
