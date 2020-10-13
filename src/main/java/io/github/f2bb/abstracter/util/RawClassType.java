package io.github.f2bb.abstracter.util;

import java.lang.reflect.Type;

// todo add sign?
public class RawClassType implements Type {
	private final String desc;

	public RawClassType(String desc) {this.desc = desc;}

	@Override
	public String toString() {
		return this.desc;
	}
}
