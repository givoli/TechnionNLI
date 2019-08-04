package com.ofergivoli.ojavalib.reflection;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;


public class ReflectionUtilsTest {


    static class A {
        private int a;
        static int s;
    }

    static class B extends A {
        String x;
        public int y;
        protected int z;
    }

    @Test
    public void getAllFieldsOfClassTest() throws Exception {

        List<Field> allFieldsOfClassInstances;

        allFieldsOfClassInstances = ReflectionUtils.getAllFieldsOfClass(B.class,true, false);
        assertEquals(fieldsToSortedFieldNames(allFieldsOfClassInstances),Arrays.asList("a","s", "x", "y", "z"));

        allFieldsOfClassInstances = ReflectionUtils.getAllFieldsOfClass(B.class,false, true);
        assertEquals(fieldsToSortedFieldNames(allFieldsOfClassInstances),Arrays.asList("a", "x", "y", "z"));
    }

    private List<String> fieldsToSortedFieldNames(List<Field> allFieldsOfClassInstances) {
        return allFieldsOfClassInstances.stream().map(f->f.getName()).collect(Collectors.toList());
    }
}