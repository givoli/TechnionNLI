package il.ac.technion.nlp.nli.parser.type_system;

import com.ofergivoli.ojavalib.data_structures.map.SafeBiMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeHashBiMap;
import edu.stanford.nlp.sempre.CanonicalNames;
import edu.stanford.nlp.sempre.NameValue;
import edu.stanford.nlp.sempre.SemType;
import edu.stanford.nlp.sempre.Value;
import edu.stanford.nlp.sempre.ValueFormula;
import il.ac.technion.nlp.nli.core.reflection.GeneralReflection;
import il.ac.technion.nlp.nli.core.state.NliEntity;
import il.ac.technion.nlp.nli.parser.NameValuesManager.NameValueType;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class InstructionTypeSystem {


    /**
     * Specifies {@link edu.stanford.nlp.sempre.AtomicSemType#name}s.
     */
    public static class AtomicTypeNames {
        /**
         * This type represents the set of all the {@link NliEntity} types.
         */
        public static final String NLI_ENTITY_TYPE = "nliEntityType";
        /**
         * This type represents the set of all primitive entity values and {@link NliEntity} values.
         * This type will not be used for {@link ValueFormula} that its {@link Value} was created by Sempre.
         */
        public static final String USER_ENTITY = "userEntity";
        /**
         * A sub-type of {@link #USER_ENTITY}.
         */
        public static final String NLI_ENTITY = "nliEntity";
        public static final String NLI_METHOD = "nliMethod";
        /**
         * This is the type of {@link NameValue} of type {@link NameValueType#STRING}. Note that sempre also uses
         * {@link edu.stanford.nlp.sempre.StringValue}. Both are represented by the same {@link SemType}.
         */
        public static final String STRING = CanonicalNames.TEXT;

    }




    private SafeBiMap<String, Class<?>> typeIdToType = new SafeHashBiMap<>();


    /**
     * Used in order to avoid collisions with type ids given by Sempre's logic.
     */
    private final String ID_PREFIX_FOR_USER_ENTITY_TYPES = "_";




    /**
     * This method always returns the same id for the same type (when using the same {@link InstructionTypeSystem}
     * object).
     * @param type a User Entity Type.
     */
    public String createTypeIdFromUserEntityType(Class<?> type) {

        assert GeneralReflection.isUserEntityType(type);


        String result = typeIdToType.safeInverseGet(type);
        if (result != null)
            return result;

        // we need to create a new id.

        String candidateId = getFriendlyIdForUserEntityType(type);
        if (typeIdToType.safeContainsKey(candidateId)){
            // nice short id already taken.
            candidateId = getLongIdForUserEntityType(type);
        }

        typeIdToType.putNewKey(candidateId, type);

        return candidateId;
    }


    private String getLongIdForUserEntityType(Class<?> type){
        return ID_PREFIX_FOR_USER_ENTITY_TYPES + type.getName();
    }

    private String getFriendlyIdForUserEntityType(Class<?> type){
        return ID_PREFIX_FOR_USER_ENTITY_TYPES + type.getSimpleName();
    }


}
