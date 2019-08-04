package il.ac.technion.nlp.nli.parser.features;

import edu.stanford.nlp.sempre.Derivation;
import edu.stanford.nlp.sempre.Example;
import edu.stanford.nlp.sempre.FeatureComputer;
import il.ac.technion.nlp.nli.parser.experiment.ExperimentRunner;
import il.ac.technion.nlp.nli.parser.general.Utils;


/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class InstructionFeatureComputer implements FeatureComputer {

    public static final String INSTRUCTION_FEATURES_DOMAIN_IN_SEMPRE = "I_";

    public static boolean isInstructionFeature(String featureFullName) {
        return featureFullName.startsWith(INSTRUCTION_FEATURES_DOMAIN_IN_SEMPRE);
    }

    public static void addFeature(Example sempreExample,  Derivation deriv, String feature) {
        deriv.addFeature(INSTRUCTION_FEATURES_DOMAIN_IN_SEMPRE,feature);
        ExperimentRunner.getCurrentExperiment().analysis
                .reportExtractedInstructionFeatureNotFilteredOut(sempreExample, deriv, feature);
    }


    @Override
    public void extractLocal(Example sempreExample, Derivation deriv) {

        // Decided not to use the state-delta based features: they didn't help.
//        if (ExperimentRunner.getCurrentExperiment().instructionFeatureComputerSettings.useAppLogicFiltering)
//            DenotationFeatureUtils.extractAppExecutionFeaturesToBeConcatenatedWithPhraseFeatures(sempreExample, deriv);

        if (Utils.derivationIsRoot(sempreExample, deriv)){
            extractMiscellaneousFeaturesFromRootDeriv(deriv);
        }

    }

    private void extractMiscellaneousFeaturesFromRootDeriv(Derivation rootDeriv) {

        if (!ExperimentRunner.getCurrentExperiment().settings.enableInstructionFeatures)
            return;

        // extract logical form size related features:
        int size = GeneralFeatureUtils.calculateFormulaSize(rootDeriv.formula);
        final int MIN_FORMULA_SIZE = 2;
        for (int i = MIN_FORMULA_SIZE; i<size; i++){
            rootDeriv.addFeature(INSTRUCTION_FEATURES_DOMAIN_IN_SEMPRE, "formula_size>" + i);
        }
    }

}
