package com.ofergivoli.ojavalib.reflection;

import com.ofergivoli.ojavalib.data_structures.map.SafeHashMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class ReflectionUtils {

    /**
     * @return all methods of clazz, including inherited fields, and regardless of access modifier.
     * @param setAccessible When true, this method performs setAccessible(true) on all the returned fields (which is
     *                      necessary if you wish to access value of private fields via reflection).
     */
    public static List<Field> getAllFieldsOfClass(Class<?> clazz, boolean includeStaticFields, boolean setAccessible) {
        List<Field> fields = new LinkedList<>();
        appendFieldsOfClassInstances_aux(clazz, includeStaticFields, fields);
        if (setAccessible)
            fields.forEach(f->f.setAccessible(true));
        return fields;
    }

    public static Object getValueOfFieldSettingAccessible(Field field, Object obj) {
        field.setAccessible(true);
        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void appendFieldsOfClassInstances_aux(Class<?> clazz, boolean includeStaticFields, List<Field> fields) {
        Class superClass = clazz.getSuperclass();
        if(superClass != null)
            appendFieldsOfClassInstances_aux(superClass, includeStaticFields, fields);
        for (Field field : clazz.getDeclaredFields()) {
            if (includeStaticFields || !java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                fields.add(field);
        }
    }

    /**
     * @return all methods of clazz, including inherited methods, and regardless of access modifier.
     * @param setAccessible When true, this method performs setAccessible(true) on all the returned methods (which is
     *                      necessary if you wish to invoke private methods via reflection).
     */
    public static List<Method> getAllMethodsOfClass(Class<?> clazz, boolean setAccessible) {
        List<Method> methods = new LinkedList<>();
        appendMethodsOfClassInstances_aux(clazz, methods);
        if (setAccessible)
            methods.forEach(m->m.setAccessible(true));
        return methods;
    }

    private static void appendMethodsOfClassInstances_aux(Class<?> clazz, List<Method> methods) {
        Class superClass = clazz.getSuperclass();
        if(superClass != null)
            appendMethodsOfClassInstances_aux(superClass, methods);
        for (Method method : clazz.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isStatic(method.getModifiers()))
                methods.add(method);
        }
    }

    private static final SafeMap<String, Class<?>> canonicalNameOfPrimitiveTypeToClass;
    static {
        canonicalNameOfPrimitiveTypeToClass = new SafeHashMap<>();
        Arrays.asList(byte.class, short.class, int.class, long.class, float.class, double.class, boolean.class,
                char.class)
                .forEach(type->canonicalNameOfPrimitiveTypeToClass.put(type.getCanonicalName(), type));
    }

    /**
     * Just like {@link Class#forName(String)} but handles primitive types as well.
     *
     */
    public static Class<?> getClassByName(String name) {
        Class<?> res = canonicalNameOfPrimitiveTypeToClass.safeGet(name);
        if (res != null)
            return res;
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

}
