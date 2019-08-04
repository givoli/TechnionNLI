package il.ac.technion.nlp.nli.parser.general;

import il.ac.technion.nlp.nli.core.dataset.Example;
import il.ac.technion.nlp.nli.parser.experiment.ExperimentSettings;
import il.ac.technion.nlp.nli.parser.InstructionKnowledgeGraph;
import il.ac.technion.nlp.nli.parser.features.PhraseAssociation;
import il.ac.technion.nlp.nli.parser.lexicon.Lexicon;
import il.ac.technion.nlp.nli.parser.type_system.InstructionTypeSystem;

/**
 * Meant to be collected by the GC after the inference is done.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class InferenceData {

    public final Example example;
    public final InstructionKnowledgeGraph graph;
    public final PhraseAssociation phraseAssociation;
    public final InstructionTypeSystem instructionTypeSystem = new InstructionTypeSystem();
    public final Lexicon lexicon;


    public InferenceData(ExperimentSettings settings, Example example, edu.stanford.nlp.sempre.Example sempreExample) {
        this.example = example;
        this.graph = (InstructionKnowledgeGraph)sempreExample.context.graph;
        phraseAssociation = new PhraseAssociation(graph,
                settings.enableInstructionFeatures && settings.useDescriptionPhraseFeatures);
        lexicon = new Lexicon(graph, phraseAssociation, settings.deterministic);
    }

}
