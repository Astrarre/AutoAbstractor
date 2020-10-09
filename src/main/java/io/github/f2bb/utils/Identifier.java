package io.github.f2bb.utils;

import java.util.Objects;

public class Identifier {
	private final String desc, name;

	public Identifier(String name, String desc) {
		this.desc = desc;
		this.name = name;
	}

	public String getDesc() {
		return this.desc;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Identifier)) {
			return false;
		}

		Identifier that = (Identifier) o;

		if (!Objects.equals(this.desc, that.desc)) {
			return false;
		}
		return Objects.equals(this.name, that.name);
	}

	@Override
	public int hashCode() {
		int result = this.desc != null ? this.desc.hashCode() : 0;
		result = 31 * result + (this.name != null ? this.name.hashCode() : 0);
		return result;
	}
}
