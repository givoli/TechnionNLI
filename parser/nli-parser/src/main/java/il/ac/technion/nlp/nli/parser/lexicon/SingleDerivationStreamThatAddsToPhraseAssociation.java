package il.ac.technion.nlp.nli.parser.lexicon;

import com.google.common.base.Verify;
import edu.stanford.nlp.sempre.Derivation;
import edu.stanford.nlp.sempre.SingleDerivationStream;
import edu.stanford.nlp.sempre.Value;
import edu.stanford.nlp.sempre.ValueFormula;
import il.ac.technion.nlp.nli.parser.experiment.ExperimentRunner;

public abstract class SingleDerivationStreamThatAddsToPhraseAssociation extends SingleDerivationStream {

    private final String phrase;

    public SingleDerivationStreamThatAddsToPhraseAssociation(String phrase) {
        this.phrase = phrase;
    }

    @Override
    public Derivation next() {
        addAssociationForNextDerivation();
        return super.next();
    }

    @Override
    public Derivation peek() {
        addAssociationForNextDerivation();
        return super.peek();
    }

    private void addAssociationForNextDerivation(){
        if (!ExperimentRunner.isExperimentCurrentlyRunning())
            return;
        Derivation derivation = super.peek();
        Verify.verify(derivation.formula instanceof ValueFormula);
        Value value = ((ValueFormula)derivation.formula).value;
        ExperimentRunner.getCurrentExperiment().getCurrentInferenceData().phraseAssociation
                .addAssociation(value, phrase, false);
    }
}
