package il.ac.technion.nlp.nli.parser;

import com.google.common.base.Verify;
import edu.stanford.nlp.sempre.Derivation;
import edu.stanford.nlp.sempre.DerivationPruner;
import edu.stanford.nlp.sempre.DerivationPruningComputer;
import edu.stanford.nlp.sempre.Example;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.parser.experiment.ExperimentRunner;
import il.ac.technion.nlp.nli.parser.denotation.DenotationUtils;
import il.ac.technion.nlp.nli.parser.features.denotation.StateDeltaFeatureGenerator;
import il.ac.technion.nlp.nli.parser.general.Utils;
import org.jetbrains.annotations.Nullable;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class InstructionDerivationPruningComputer extends DerivationPruningComputer {


    public InstructionDerivationPruningComputer(DerivationPruner pruner) {
        super(pruner);
    }

    @Override
    public boolean isPruned(Derivation deriv) {

        return shouldDerivationBePrunedDueToAppLogicSignal(deriv);

    }



    public static boolean shouldDerivationBePrunedDueToAppLogicSignal(Derivation deriv){


        if (!ExperimentRunner.getCurrentExperiment().settings.useAppLogicFiltering)
            return false;


        //TODO: don't use analysis logic here.
        Example sempreExample = ExperimentRunner.getCurrentExperiment().analysis.currentSempreExampleBeingParsed;
        //noinspection ConstantConditions
        Verify.verify(sempreExample.id.equals(
                ExperimentRunner.getCurrentExperiment().getCurrentExampleBeingParsed().getId()));


        if (!Utils.derivationIsRoot(sempreExample, deriv))
            return false;


        @Nullable State endState = DenotationUtils.getEndStateValueDenotedByDerivation(sempreExample, deriv).getState();
        if (endState == null) {
            // invalid state
            return true;
        }

        InstructionKnowledgeGraph graph = (InstructionKnowledgeGraph) sempreExample.context.graph;
        boolean deterministic = ExperimentRunner.getCurrentExperiment().settings.deterministic;
        return new StateDeltaFeatureGenerator(sempreExample, deriv, graph, endState,deterministic).isEmpty();
    }

}
