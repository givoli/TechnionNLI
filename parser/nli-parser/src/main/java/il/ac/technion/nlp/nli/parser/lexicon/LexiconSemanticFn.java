package il.ac.technion.nlp.nli.parser.lexicon;

import edu.stanford.nlp.sempre.*;
import fig.basic.LispTree;
import il.ac.technion.nlp.nli.core.state.NliEntity;
import il.ac.technion.nlp.nli.core.state.PrimitiveEntity;
import il.ac.technion.nlp.nli.parser.experiment.ExperimentRunner;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

/**
 * NOTICE: The content of this class was copied from ppasupat's {@link FuzzyMatchFn} and has been modified.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class LexiconSemanticFn extends SemanticFn{

    public enum Mode {

        // anchored:
        ANCHORED_STRING_ENTITY,
        ANCHORED_ENUM_VALUE,


        // non-anchored:
        /**
         * For relations where the second argument represents a {@link PrimitiveEntity}.
         */
        PRIMITIVE_RELATION,
        /**
         * For relations where the second argument represents an {@link NliEntity}.
         */
        NLI_ENTITY_RELATION,
        NLI_METHOD_NAME,
        ALL_NLI_ENTITIES_PER_TYPE
    }

    private Mode mode;

    @Override
    public void init(LispTree tree) {
        super.init(tree);
        mode = Mode.valueOf(tree.child(1).value);
    }

    @Override
    public DerivationStream call(Example ex, Callable c) {
        return new LazyFuzzyMatchFnDerivs(ex, c, mode);
    }

    public static class LazyFuzzyMatchFnDerivs extends MultipleDerivationStream {
        final KnowledgeGraph graph;

        /**
         * A value that needs to be passed to {@link Derivation.Builder}, not used by this class otherwise.
         */
        final Callable sempreCallable;

        /**
         * Null in case {@link #mode} is a non-anchor derivation (i.e. derivation "from thin air").
         */
        final @Nullable String phrase;
        final Mode mode;

        int index = 0;
        @Nullable Lexicon.DerivedFormulas derivedFormulas;

        public LazyFuzzyMatchFnDerivs(Example ex, Callable sempreCallable, Mode mode) {
            this.graph = ex.context.graph;
            this.sempreCallable = sempreCallable;
            this.phrase = sempreCallable.getChildren().isEmpty() ? null : sempreCallable.childStringValue(0);
            this.mode = mode;
        }

        @Override
        public Derivation createDerivation() {

            // Compute the formulas if not computed yet
            if (derivedFormulas == null) {
                if (phrase == null)
                    derivedFormulas = ExperimentRunner.getCurrentExperiment().getCurrentInferenceData().lexicon
                            .getNonAnchoredFormulas(mode);
                else
                    derivedFormulas = ExperimentRunner.getCurrentExperiment().getCurrentInferenceData().lexicon
                            .getAnchoredFormulas(mode, phrase);
            }

            // Use the next formula to create a derivation
            if (index >= derivedFormulas.elements.size()) return null;
            Pair<Formula, FeatureVector> formulaAndFeatures = derivedFormulas.elements.get(index++);
            SemType type = TypeInference.inferType(formulaAndFeatures.getLeft());

            return new Derivation.Builder()
                    .withCallable(sempreCallable)
                    .formula(formulaAndFeatures.getLeft())
                    .type(type)
                    .localFeatureVector(formulaAndFeatures.getRight())
                    .createDerivation();
        }

    }

}
