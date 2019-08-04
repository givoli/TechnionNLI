package il.ac.technion.nlp.nli.core.dataset;

import org.junit.Test;

import static org.junit.Assert.*;

public class DatasetTest {

    @Test
    public void removeTailingDot() {

        assertEquals("a b c", Dataset.removeTailingDot("a b c"));
        assertEquals("a b c", Dataset.removeTailingDot("a b c."));
    }
}