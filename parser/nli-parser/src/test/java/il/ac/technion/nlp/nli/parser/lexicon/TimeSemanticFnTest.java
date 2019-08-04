package il.ac.technion.nlp.nli.parser.lexicon;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class TimeSemanticFnTest {

    @SuppressWarnings("ConstantConditions")
    @Test
    public void parseTimeValueFromLanguageInfoFormat() {
        assertEquals(1, TimeSemanticFn.parseTimeValueFromLanguageInfoFormat("T01:02").hour);
        assertEquals(20, TimeSemanticFn.parseTimeValueFromLanguageInfoFormat("T20:02").hour);

        assertEquals(2, TimeSemanticFn.parseTimeValueFromLanguageInfoFormat("T01:02").minute);
        assertEquals(20, TimeSemanticFn.parseTimeValueFromLanguageInfoFormat("T01:20").minute);

        assertEquals(24, TimeSemanticFn.parseTimeValueFromLanguageInfoFormat("T24:00").hour);
        assertEquals(0, TimeSemanticFn.parseTimeValueFromLanguageInfoFormat("T00:00").hour);



        assertEquals(null, TimeSemanticFn.parseTimeValueFromLanguageInfoFormat("01:02"));
        assertEquals(null, TimeSemanticFn.parseTimeValueFromLanguageInfoFormat("T01:022"));

        assertEquals(null, TimeSemanticFn.parseTimeValueFromLanguageInfoFormat("T12:60"));
        assertEquals(null, TimeSemanticFn.parseTimeValueFromLanguageInfoFormat("T12:-1"));
        assertEquals(null, TimeSemanticFn.parseTimeValueFromLanguageInfoFormat("T24:01"));
        assertEquals(null, TimeSemanticFn.parseTimeValueFromLanguageInfoFormat("T25:01"));
        assertEquals(null, TimeSemanticFn.parseTimeValueFromLanguageInfoFormat("T-1:01"));
    }
}