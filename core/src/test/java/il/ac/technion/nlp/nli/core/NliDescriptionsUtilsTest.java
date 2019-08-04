package il.ac.technion.nlp.nli.core;

import il.ac.technion.nlp.nli.core.state.NliEntity;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class NliDescriptionsUtilsTest {


    public static class Dummy implements NliEntity {
        private static final long serialVersionUID = 2082702798553945878L;

        @EnableNli
        @NliDescriptions(descriptions = {"a", "Hello World"})
        public void someMethod() {
        }

        @NliDescriptions(descriptions = {"b", "Hello Again"})
        public List<String> someField;
    }

    @Test
    public void testGenerateDescriptionsForMethod() throws NoSuchMethodException {
        String METHOD_NAME = "someMethod";
        String DESCRIPTION_FROM_METHOD_NAME = "some method";

        Method method = Dummy.class.getDeclaredMethod(METHOD_NAME);
        List<String> sortedDescriptions;

        sortedDescriptions = NliDescriptionsUtils.generateDescriptionsForMethod(method, false).stream().sorted()
                .collect(Collectors.toList());
        assertEquals(sortedDescriptions, Stream.of(DESCRIPTION_FROM_METHOD_NAME).sorted().collect(Collectors.toList()));

        sortedDescriptions = NliDescriptionsUtils.generateDescriptionsForMethod(method, true).stream().sorted()
                .collect(Collectors.toList());
        assertEquals(sortedDescriptions, Stream.of("a", "hello world",DESCRIPTION_FROM_METHOD_NAME).sorted()
                .collect(Collectors.toList()));

    }

    @Test
    public void testGenerateDescriptionsForField() throws NoSuchFieldException {
        String FIELD_NAME = "someField";
        String DESCRIPTION_FROM_FIELD_NAME = "some field";


        Field field = Dummy.class.getDeclaredField(FIELD_NAME);

        List<String> sortedDescriptions;

        sortedDescriptions = NliDescriptionsUtils.generateDescriptionsForRelationField(field, false).stream().sorted()
                .collect(Collectors.toList());
        assertEquals(sortedDescriptions, Stream.of(DESCRIPTION_FROM_FIELD_NAME).sorted().collect(Collectors.toList()));

        sortedDescriptions = NliDescriptionsUtils.generateDescriptionsForRelationField(field, true).stream().sorted()
                .collect(Collectors.toList());
        assertEquals(sortedDescriptions, Stream.of("b", "hello again",DESCRIPTION_FROM_FIELD_NAME).sorted()
                .collect(Collectors.toList()));

    }

}