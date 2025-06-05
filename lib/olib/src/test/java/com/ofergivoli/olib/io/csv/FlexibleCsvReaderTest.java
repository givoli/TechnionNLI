package ofergivoli.olib.io.csv;

import com.google.common.base.Verify;
import ofergivoli.olib.test_utils.TestUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class FlexibleCsvReaderTest {


    public static class ClassWithPrimitiveFields {
        private int a;
        public int b;

        public ClassWithPrimitiveFields() {
        }

        public ClassWithPrimitiveFields(int a, int b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClassWithPrimitiveFields that = (ClassWithPrimitiveFields) o;
            return a == that.a &&
                    b == that.b;
        }

        @Override
        public int hashCode() {

            return Objects.hash(a, b);
        }

        @Override
        public String toString() {
            return "[" + "a=" + a + ", b=" + b + ']';
        }
    }



    public static class ClassWithNonPrimitiveFields {

        public static Integer staticField1 = 1; // ignored
        public static int staticField2 = 2; // ignored


        private Integer i_nullable;
        public String s;
        public Double d;
        private Boolean b;
        private Long l;

        public ClassWithNonPrimitiveFields() {
        }

        public ClassWithNonPrimitiveFields(@Nullable Integer i_nullable, String s, Double d, Boolean b, Long l) {
            this.i_nullable = i_nullable;
            this.s = s;
            this.d = d;
            this.b = b;
            this.l = l;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClassWithNonPrimitiveFields that = (ClassWithNonPrimitiveFields) o;
            return Objects.equals(i_nullable, that.i_nullable) &&
                    Objects.equals(s, that.s) &&
                    Objects.equals(d, that.d) &&
                    Objects.equals(b, that.b) &&
                    Objects.equals(l, that.l);
        }

        @Override
        public int hashCode() {

            return Objects.hash(i_nullable, s, d, b, l);
        }

        @Override
        public String toString() {
            return "[" + "i_nullable=" + i_nullable + ", s='" + s + '\'' + ", d=" + d + ", b=" + b + ", l=" + l +
                    ']';
        }
    }


    public static class ClassWithNoZeroParametersConstructor {
        List<String> s;
        @SuppressWarnings("unused")
        ClassWithNoZeroParametersConstructor(int x){
        }
    }

    public static class ClassWithInvalidFieldType {
        List<String> s;
    }


    @SuppressWarnings("FieldCanBeLocal")
    private static String RESOURCE_DIR_RELATIVE_TO_TEST_RESOURCE_DIR = "io/csv/FlexibleCsvReader";

    private final Path validCsvForClassContainingNonPrimitiveFields = getCsv("ClassWithNonPrimitiveFields/valid.csv");



    /**
     * Also tests for logic NOT related to the type of the fields (e.g. empty lines and the separator in column titles).
     */
    @Test
    public void testWitClassContainingPrimitiveFields() {

        // Testing a csv with a column that has no existing field:
        List<ClassWithPrimitiveFields> objects = FlexibleCsvReader.readAll(
                getCsv("ClassWithPrimitiveFields/unexsiting_A_B.csv"),
                ClassWithPrimitiveFields.class, true, true, false, false);
        assertEquals(Collections.singletonList(new ClassWithPrimitiveFields(2, 3)), objects);
        TestUtils.verifyThrow(FlexibleCsvReader.InvalidContent.class, ()->
                FlexibleCsvReader.readAll(getCsv("ClassWithPrimitiveFields/unexsiting_A_B.csv"),
                        ClassWithPrimitiveFields.class, true, false, false, false));


        // Testing a csv with a missing column:
        objects = FlexibleCsvReader.readAll(getCsv("ClassWithPrimitiveFields/A.csv"), ClassWithPrimitiveFields.class,
                true, false, true, false);
        assertEquals(Arrays.asList(new ClassWithPrimitiveFields(1, 0), new ClassWithPrimitiveFields(2, 0)), objects);
        TestUtils.verifyThrow(FlexibleCsvReader.InvalidContent.class, ()->
                FlexibleCsvReader.readAll(getCsv("ClassWithPrimitiveFields/A.csv"),
                        ClassWithPrimitiveFields.class, true, false, false, false));


        // testing skipping empty lines feature:
        objects = FlexibleCsvReader.readAll(getCsv("ClassWithPrimitiveFields/A__withEmptyLines.csv"),
                ClassWithPrimitiveFields.class, true, false, true, true);
        assertEquals(Arrays.asList(new ClassWithPrimitiveFields(1, 0), new ClassWithPrimitiveFields(2, 0)), objects);
        // Can't have a blank cell in column representing a field of primitive type:
        TestUtils.verifyThrow(FlexibleCsvReader.InvalidContent.class, ()->
                FlexibleCsvReader.readAll(getCsv("ClassWithPrimitiveFields/A__withEmptyLines.csv"),
                        ClassWithPrimitiveFields.class, true, false, true, false));


        // invalid cell content (float):
        TestUtils.verifyThrow(FlexibleCsvReader.InvalidContent.class, ()->
                FlexibleCsvReader.readAll(getCsv("ClassWithPrimitiveFields/A__withBadType.csv"),
                        ClassWithPrimitiveFields.class, true, true, true, true));

        // invalid cell content ("NA"):
        TestUtils.verifyThrow(FlexibleCsvReader.InvalidContent.class, ()->
                FlexibleCsvReader.readAll(getCsv("ClassWithPrimitiveFields/A__withStringRepresentingNull.csv"),
                        ClassWithPrimitiveFields.class, true, false, true, true, ";", "NA"));

    }


    /**
     * Also test for string representations of boolean values.
     */
    @Test
    public void testWitClassContainingNonPrimitiveFields() {

        List<ClassWithNonPrimitiveFields> objects = FlexibleCsvReader.readAll(
                validCsvForClassContainingNonPrimitiveFields,
                ClassWithNonPrimitiveFields.class, true, false, false, false, ";", "NA");
        assertEquals(Arrays.asList(
                new ClassWithNonPrimitiveFields(null, "hello", 0.5, true, 0L),
                new ClassWithNonPrimitiveFields(null, "", -0.5, true, -1L),
                new ClassWithNonPrimitiveFields(3,    "NA", 0.0, true, 9223372036854775807L),
                new ClassWithNonPrimitiveFields(0,    "hello", null, false, 0L),
                new ClassWithNonPrimitiveFields(0,    "hello", 0.0, false, 0L),
                new ClassWithNonPrimitiveFields(0,    "hello", 0.0, false, 0L)),
                objects);

        assertEquals(1, (int)ClassWithNonPrimitiveFields.staticField1); // verifying it didn't change.
        assertEquals(2, ClassWithNonPrimitiveFields.staticField2); // verifying it didn't change.

        TestUtils.verifyThrow(FlexibleCsvReader.InvalidContent.class, ()->FlexibleCsvReader.readAll(
                getCsv("ClassWithNonPrimitiveFields/invalid_boolean.csv"),
                ClassWithNonPrimitiveFields.class, true, false, false, false, ";", "NA"));
        TestUtils.verifyThrow(FlexibleCsvReader.InvalidContent.class, ()->FlexibleCsvReader.readAll(
                getCsv("ClassWithNonPrimitiveFields/invalid_integer.csv"),
                ClassWithNonPrimitiveFields.class, true, false, false, false, ";", "NA"));
    }

    @Test
    public void testForExceptionInCaseNoZeroParameterConstructor(){
        TestUtils.verifyThrow(FlexibleCsvReader.InvalidRowObjectType.class, ()->
                FlexibleCsvReader.readAll(validCsvForClassContainingNonPrimitiveFields,
                ClassWithNoZeroParametersConstructor.class, true, true, true, true, ";", "NA"));
    }


    @Test
    public void testForExceptionInCaseOfInvalidFieldType(){
        TestUtils.verifyThrow(FlexibleCsvReader.InvalidRowObjectType.class, ()->
                FlexibleCsvReader.readAll(validCsvForClassContainingNonPrimitiveFields,
                ClassWithInvalidFieldType.class, true, true, true, true, ";", "NA"));
    }



    private Path getCsv(String pathRelativeToResourceDirOfThisClass) {
        try {
            URL resourceDir = getClass().getClassLoader().getResource(RESOURCE_DIR_RELATIVE_TO_TEST_RESOURCE_DIR);
            if (resourceDir == null)
                throw new RuntimeException("Can't find resource directory: " + RESOURCE_DIR_RELATIVE_TO_TEST_RESOURCE_DIR);
            Path result = Paths.get(new File(resourceDir.toURI()).getPath())
                    .resolve(pathRelativeToResourceDirOfThisClass);
            Verify.verify(Files.isRegularFile(result));
            return result;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}