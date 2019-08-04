package il.ac.technion.nlp.nli.parser.features;

import edu.stanford.nlp.sempre.*;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class GeneralFeatureUtilsTest {

    private ValueFormula primitiveFormula1 = new ValueFormula<>(new NameValue("1"));
    private ValueFormula primitiveFormula2 = new ValueFormula<>(new NameValue("2"));
    private ValueFormula primitiveFormula3 = new ValueFormula<>(new NameValue("3"));

    Formula nonPrmitiveFormula = new JoinFormula(primitiveFormula1, primitiveFormula2);
    Formula rootFormula = new JoinFormula(nonPrmitiveFormula, primitiveFormula3);

    @Test
    public void testCalculateFormulaSize() {

        assertEquals(3, GeneralFeatureUtils.calculateFormulaSize(nonPrmitiveFormula));
        assertEquals(5, GeneralFeatureUtils.calculateFormulaSize(rootFormula));
    }

    @Test
    public void testGetLocalValuesOfDerivation() {

        Derivation deriv = GeneralFeatureTestUtils.createDerivation(nonPrmitiveFormula);
        Derivation rootDeriv = GeneralFeatureTestUtils.createDerivation(rootFormula);
        rootDeriv.children.add(deriv);
        assertEquals(Collections.singletonList(primitiveFormula3.value),
                GeneralFeatureUtils.getLocalPrimitiveValuesOfDerivation(rootDeriv, true));
    }
}