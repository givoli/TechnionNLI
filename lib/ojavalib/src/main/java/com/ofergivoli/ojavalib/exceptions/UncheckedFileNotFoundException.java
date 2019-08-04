package com.ofergivoli.ojavalib.exceptions;

public class UncheckedFileNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 4596293758961204802L;

	public UncheckedFileNotFoundException(Throwable cause) {
		super(cause);
	}

	public UncheckedFileNotFoundException() {
	}
}
