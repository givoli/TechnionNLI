package com.ofergivoli.ojavalib.general;

/**
 * General functionality for Java programing.
 */
public class General {


    /**
     * @throws NullPointerException in case val is null
     */
	@Deprecated //Use Guava's Verify.verifyNotNull instead.
    public static <T> void verifyNotNull(T val) {
        if (val == null)
            throw new NullPointerException();
    }

    /**
     * Throws a RuntimeException if val is false.
     */
	@Deprecated //Use Guava's Verify.verify instead.
    public static void expect(boolean val) {
        if (!val)
            throw new RuntimeException("expected a true value.");
    }
}
