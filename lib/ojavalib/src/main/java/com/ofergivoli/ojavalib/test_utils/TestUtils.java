package com.ofergivoli.ojavalib.test_utils;

import static org.junit.Assert.fail;

public class TestUtils {

    /**
     * Verifies 'runnable' throws an exception of type expectedExceptionType (or a subtype).
     * Throws an exception otherwise.
     */
    public static void verifyThrow(Class<? extends RuntimeException> expectedExceptionType, Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException e) {
            if (expectedExceptionType.isAssignableFrom(e.getClass()))
                return;
            fail("Expected exception " + expectedExceptionType + ", got:" + e.getClass());
        }
        throw new RuntimeException("No exception thrown. Expected exception of type " + expectedExceptionType);
    }


    /**
     * Verifies 'runnable' throws an exception of type {@link RuntimeException} (or a subtype).
     * Throws an exception otherwise.
     */
    public static void verifyThrow(Runnable runnable) {
        verifyThrow(RuntimeException.class, runnable);
    }

}
