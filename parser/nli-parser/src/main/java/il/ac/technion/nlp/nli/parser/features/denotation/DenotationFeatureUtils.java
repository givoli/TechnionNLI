package il.ac.technion.nlp.nli.parser.features.denotation;

import edu.stanford.nlp.sempre.Derivation;
import edu.stanford.nlp.sempre.Example;
import edu.stanford.nlp.sempre.tables.features.PhrasePredicateFeatureComputer;
import edu.stanford.nlp.sempre.tables.features.PredicateInfo;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.parser.experiment.ExperimentRunner;
import il.ac.technion.nlp.nli.parser.InstructionKnowledgeGraph;
import il.ac.technion.nlp.nli.parser.denotation.DenotationUtils;
import il.ac.technion.nlp.nli.parser.features.InstructionFeatureComputer;
import il.ac.technion.nlp.nli.parser.general.Utils;

import java.util.List;
import java.util.stream.Collectors;

public class DenotationFeatureUtils {

    // TODO: delete if not used.
    public static void extractAppExecutionFeaturesToBeConcatenatedWithPhraseFeatures(Example sempreExample,
                                                                                     Derivation deriv) {

        if (!Utils.derivationIsRoot(sempreExample, deriv))
            return;

        State endState = DenotationUtils.getEndStateValueDenotedByDerivation(sempreExample, deriv).getState();
        if (endState == null) {
            // invalid state
            return;
        }

        List<String> features = new StateDeltaFeatureGenerator(sempreExample, deriv,
                ((InstructionKnowledgeGraph) sempreExample.context.graph), endState,
                ExperimentRunner.getCurrentExperiment().settings.deterministic)
                .generateFeaturesToBeConcatenatedWithPhraseFeatures();

        // Adding the non-concatenated version of the features:
        features.forEach(feature->
                InstructionFeatureComputer.addFeature(sempreExample, deriv, feature));

        List<PredicateInfo> predicates = features.stream()
                .map(feature->new PredicateInfo(feature, PredicateInfo.PredicateType.SUBFEATURE))
                .collect(Collectors.toList());

        PhrasePredicateFeatureComputer.extractPhrasePredicateFeatures(sempreExample, deriv, predicates);
    }

}
