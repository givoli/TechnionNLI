    package il.ac.technion.nlp.nli.parser.type_system;

import com.ofergivoli.ojavalib.data_structures.map.SafeMap;
import com.ofergivoli.ojavalib.data_structures.set.SafeSet;
import edu.stanford.nlp.sempre.*;
import il.ac.technion.nlp.nli.parser.experiment.ExperimentRunner;
import il.ac.technion.nlp.nli.parser.InstructionKnowledgeGraph;
import il.ac.technion.nlp.nli.parser.NameValuesManager;

/**
 * Logic for mapping {@link NameValue} to {@link SemType}.
 *
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class InstructionTypeLookup implements TypeLookup {

    /**
     * Uses {@link ExperimentRunner#getCurrentExperiment()}.
     */
    @Override
    public SemType getEntityType(String nameValueId) {

        InstructionKnowledgeGraph graph = ExperimentRunner.getCurrentExperiment().getCurrentInferenceData().graph;
        NameValuesManager nameValuesManager = graph.nameValuesManager;
        InstructionTypeSystem instructionTypeSystem = ExperimentRunner.getCurrentExperiment()
                .getCurrentInferenceData().instructionTypeSystem;

        NameValue nameValue = new NameValue(nameValueId);
        String atomicTypeName;
        NameValuesManager.NameValueType nameValueType = nameValuesManager.getNameValueType(nameValue);
        if (nameValueType == null)
            return null;

        switch (nameValueType) {
            case FIELD_RELATION:
            case NON_FIELD_RELATION:
                return null;

            case ENUM:
                atomicTypeName = instructionTypeSystem.createTypeIdFromUserEntityType(
                        nameValuesManager.getEnumValue(nameValue).getDeclaringClass());
                break;

            case NLI_METHOD_NAME:
                atomicTypeName = InstructionTypeSystem.AtomicTypeNames.NLI_METHOD;
                break;

            case NLI_ENTITY:
                atomicTypeName = instructionTypeSystem.createTypeIdFromUserEntityType(
                        graph.initialState.getEntityById(nameValuesManager.getNliEntityId(nameValue)).getClass());
                break;

            case NLI_ENTITY_TYPE:
                atomicTypeName = InstructionTypeSystem.AtomicTypeNames.NLI_ENTITY_TYPE;
                break;

            case STRING:
                atomicTypeName = InstructionTypeSystem.AtomicTypeNames.STRING;
                break;

            default:
                throw new Error();
        }

        return SemType.newUnionSemType(atomicTypeName);
    }


    /**
     * Uses {@link ExperimentRunner#getCurrentExperiment()}.
     * If there are no facts (triples) with that relation, we return null (this makes the implementation of this
     * method a lot easier).
     */
    @Override
    public SemType getPropertyType(String nameValueId) {

        NameValue nameValue = new NameValue(nameValueId);

        NameValuesManager.NameValueType nameValueType = ExperimentRunner.getCurrentExperiment()
                .getCurrentInferenceData().graph.nameValuesManager.getNameValueType(nameValue);
        if (nameValueType == null)
            return null;

        switch (nameValueType) {
            case ENUM:
            case NLI_METHOD_NAME:
            case NLI_ENTITY:
            case NLI_ENTITY_TYPE:
            case STRING:
                return null;

            case FIELD_RELATION:
            case NON_FIELD_RELATION:

                SafeMap<NameValue, SafeSet<Value>> firstArgToSecondArgs =
                    ExperimentRunner.getCurrentExperiment().getCurrentInferenceData().graph.kb
                            .getRelationToFirstArgToSecondArgs().safeGet(nameValue);
                if (firstArgToSecondArgs == null)
                    return null;

                NameValue firstArgRepresentative = firstArgToSecondArgs.keySet().iterator().next();
                Value secondArgRepresentative = firstArgToSecondArgs.values().iterator().next().iterator().next();


                return new FuncSemType(getEntityType(firstArgRepresentative.id),
                        TypeInference.inferType(new ValueFormula<>(secondArgRepresentative), this));

            default:
                throw new Error();
        }
    }
}
