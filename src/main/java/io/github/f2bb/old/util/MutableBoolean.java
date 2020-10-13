package io.github.f2bb.old.util;

public class MutableBoolean {
	private boolean bool;

	public MutableBoolean(boolean bool) {
		this.bool = bool;
	}

	public MutableBoolean() {}

	public boolean isBool() {
		return this.bool;
	}

	public void setBool(boolean bool) {
		this.bool = bool;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof MutableBoolean)) {
			return false;
		}

		MutableBoolean aBoolean = (MutableBoolean) o;

		return this.bool == aBoolean.bool;
	}

	@Override
	public int hashCode() {
		return (this.bool ? 1 : 0);
	}

	@Override
	public String toString() {
		return String.valueOf(this.bool);
	}
}
