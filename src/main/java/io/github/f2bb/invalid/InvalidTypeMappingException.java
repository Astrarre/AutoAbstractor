package io.github.f2bb.invalid;

public class InvalidTypeMappingException extends IllegalStateException {
	public InvalidTypeMappingException() {
	}

	public InvalidTypeMappingException(String s) {
		super(s);
	}

	public InvalidTypeMappingException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidTypeMappingException(Throwable cause) {
		super(cause);
	}
}
