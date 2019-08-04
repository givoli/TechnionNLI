package il.ac.technion.nlp.nli.core;

import il.ac.technion.nlp.nli.core.reflection.NliMethodReflection;
import il.ac.technion.nlp.nli.core.state.NliEntity;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class NliDescriptionsUtils {

    /**
     * @return natural language phrases descriptions of an NLI method (lower case).
     * Order: from most to least important.
     */
    public static List<String> generateDescriptionsForMethod(Method nliMethod, boolean useAnnotation) {
        List<String> result = new ArrayList<>();
        assert (NliMethodReflection.isValidNliMethod(nliMethod));
        result.add(createDescriptionFromIdentifierName(nliMethod.getName()));
        if (useAnnotation) {
            NliDescriptions descriptionAnnotation = nliMethod.getAnnotation(NliDescriptions.class);
            if (descriptionAnnotation != null)
                result.addAll(generateDescriptionsFromAnnotation(descriptionAnnotation));
        }
        return result;
    }


    /**
     * @return natural language phrases descriptions of a relation field (lower case).
     * Order: from most to least important.
     */
    public static List<String> generateDescriptionsForRelationField(Field relationField, boolean useAnnotation) {
        List<String> result = new ArrayList<>();
        result.add(createDescriptionFromIdentifierName(relationField.getName()));
        if (useAnnotation) {
            NliDescriptions descriptionAnnotation = relationField.getAnnotation(NliDescriptions.class);
            if (descriptionAnnotation != null)
                result.addAll(generateDescriptionsFromAnnotation(descriptionAnnotation));
        }
        return result;
    }

    /**
     * @return natural language phrases descriptions of a {@link NliEntity} type.
     * Order: from most to least important.
     */
    public static List<String> generateDescriptionsForNliEntityType(Class<? extends NliEntity> entityType,
                                                                    boolean useAnnotation) {
        return generateDescriptionsForNliEntityTypeOrEnumType(entityType, useAnnotation);
    }

    /**
     * @return natural language phrases descriptions of an enum type.
     * Order: from most to least important.
     */
    public static List<String> generateDescriptionsForEnumType(Class<Enum<?>> enumType,
                                                               boolean useAnnotation) {
        return generateDescriptionsForNliEntityTypeOrEnumType(enumType, useAnnotation);
    }


    /**
     * @param type can be either {@link NliEntity} subtype, or an enum.
     * @return Order: from most to least important.
     */
    private static List<String> generateDescriptionsForNliEntityTypeOrEnumType(Class<?> type, boolean useAnnotation) {
        List<String> result = new ArrayList<>();
        result.add(createDescriptionFromIdentifierName(type.getSimpleName()));
        if (useAnnotation) {
            NliDescriptions descriptionAnnotation = type.getAnnotation(NliDescriptions.class);
            if (descriptionAnnotation != null)
                result.addAll(generateDescriptionsFromAnnotation(descriptionAnnotation));
        }
        return result;
    }

    /**
     * @return Order: from most to least important.
     */
    public static List<String> generateDescriptionsForEnumValue(Enum<?> enumValue) {
        return Collections.singletonList(enumValue.toString().replaceAll("_", " ").toLowerCase());
    }


    /**
     * @return Order: from most to least important.
     */
    private static List<String> generateDescriptionsFromAnnotation(NliDescriptions descriptionAnnotation) {
        return Arrays.stream(descriptionAnnotation.descriptions())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    /**
     * @param camelCaseIdentifierName the name of a field or method.
     */
    private static String createDescriptionFromIdentifierName(String camelCaseIdentifierName) {
        return StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(camelCaseIdentifierName), ' ').toLowerCase();
    }


}
