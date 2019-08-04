package com.ofergivoli.ojavalib.exceptions;

public class UncheckedClassNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 4887531828077783916L;

    public UncheckedClassNotFoundException(ClassNotFoundException e) {
        super(e);
    }

    @Deprecated
    public UncheckedClassNotFoundException() {
    }

}
