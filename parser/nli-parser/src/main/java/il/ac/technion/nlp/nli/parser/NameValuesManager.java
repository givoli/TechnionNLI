package il.ac.technion.nlp.nli.parser;

import com.google.common.base.Verify;
import com.ofergivoli.ojavalib.data_structures.map.SafeBiMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeHashBiMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeHashMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeMap;
import com.ofergivoli.ojavalib.data_structures.set.SafeHashSet;
import com.ofergivoli.ojavalib.data_structures.set.SafeSet;
import edu.stanford.nlp.sempre.CanonicalNames;
import edu.stanford.nlp.sempre.NameValue;
import edu.stanford.nlp.sempre.tables.TableTypeSystem;
import il.ac.technion.nlp.nli.core.state.NliEntity;
import il.ac.technion.nlp.nli.core.state.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import static il.ac.technion.nlp.nli.parser.NameValuesManager.NameValueType.*;

/**
 * An object of this class defines the meaning of the {@link NameValue} objects created by the logic of
 * {@link il.ac.technion.nlp.nli.parser}, and manages their creation such that no two {@link NameValue} objects with
 * different ids would represent the same value.
 *
 * Note: we try to make the ids of the {@link NameValue} objects created by the logic informative for a human trying to
 * debug the parser. But other than that when this class creates new {@link NameValue} objects it does not use the id
 * string to store formal information (as done by Sempre's logic).
 *
 */
public class NameValuesManager {

    /**
     * Evert {@link NameValue} managed by this object has one of the following types.
     */
    public enum NameValueType {
        // relation types:
        FIELD_RELATION(false),
        NON_FIELD_RELATION(false),

        // unary types:
        NLI_ENTITY_TYPE(false),
        NLI_METHOD_NAME(false),
        // of which, entity types:
        NLI_ENTITY(false),
        /**
         * Note: an enum value can also be {@link BooleanEnum} in order to represent the user values 'true' and 'false'.
         */
        ENUM(true),
        STRING(true);
        // Note: other primitive entity types are not represented in Sempre by NameValue (rather, by another Value
        // subtype).


        public final boolean representingPrimitiveEntity;

        NameValueType(boolean representingPrimitiveEntity) {
            this.representingPrimitiveEntity = representingPrimitiveEntity;
        }
    }

    private final SafeBiMap<String, Enum<?>> idToEnumValue = new SafeHashBiMap<>();
    private final SafeBiMap<String, String> idToFunctionFriendlyId = new SafeHashBiMap<>();
    private final SafeBiMap<String, String> idToNliEntityId = new SafeHashBiMap<>();
    private final SafeBiMap<String, Class<? extends NliEntity>> idToNliEntityType = new SafeHashBiMap<>();
    private final SafeBiMap<String, Field> idToFieldRelation = new SafeHashBiMap<>();
    private final SafeBiMap<String, String> idToString = new SafeHashBiMap<>();

    /**
     * Contains as keys the ids of all the {@link NameValue}s managed by this object.
     */
    private final SafeMap<String, NameValueType> idToNameValueType = new SafeHashMap<>();

    /**
     * @see #createNameValueWithUnusedId(String, boolean, NameValueType)
     */
    private SafeSet<String> usedNameValueIds = new SafeHashSet<>();


    /**
     * used in order to avoid collisions with ids given by Sempre's logic.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String PREFIX_TO_PRECEDE_BASEID = "_";






    /**
     * This method never creates multiple {@link NameValue} objects with the same id (for the same
     * {@link NameValuesManager} object with which this method is invoked).
     * @param baseId The returned {@link NameValue}'s id is based on this argument. If 'useBaseIdAsId' is true then the
     *               id is identical to 'baseId'. Otherwise:
     *                  - The id prefixed with an "_" to prevent collisions with the NameValue ids originated in
     *                  Sempre's logic.
     *                  - If necessary for uniqueness, a suffix is added.
     * @throws RuntimeException in case 'useBaseIdAsId' is true, but 'baseId' is already used as an id.
     */
    private NameValue createNameValueWithUnusedId(String baseId, boolean useBaseIdAsId, NameValueType nameValueType) {

        String id;
        if (useBaseIdAsId) {
            id = baseId;
            if (usedNameValueIds.safeContains(id))
                throw new RuntimeException("The id " + id + " is already used.");
        } else {
            id = TableTypeSystem.getUnusedName(PREFIX_TO_PRECEDE_BASEID + baseId, usedNameValueIds);
        }

        Verify.verify(usedNameValueIds.add(id));
        idToNameValueType.putNewKey(id, nameValueType);
        return new NameValue(id);
    }

    /**
     * @param nameValueWithReversibleId can have an id that contains the prefix "!" to indicate a reverse relation (this
     *                                  prefix will have no effect on the returned value).
     * @return null in case 'nameValueWithReversibleId' is not managed by this object.
     */
    public @Nullable NameValueType getNameValueType(NameValue nameValueWithReversibleId) {

        String id = getNonReversedNameValueId(nameValueWithReversibleId.id);
        return idToNameValueType.safeGet(id);
    }




    /**
     * @return the argument without the prefix "!" (indicating reversed relation) if there is one.
     */
    public static String getNonReversedNameValueId(String reversibleNameValueId){
        return CanonicalNames.isReverseProperty(reversibleNameValueId) ?
                CanonicalNames.reverseProperty(reversibleNameValueId) : reversibleNameValueId;
    }


    /**
     * If 'idToValue' doesn't already contain the value 'valueRepresentedByNameValue', then a new entry is
     * added to 'idToValue' with a new NameValue as the key.
     * @param baseId used in case 'valueRepresentedByNameValue' is not already in the 'idToValue' map.
     * @return the NameValue for the relevant entry in 'idToValue' (which might have been added by this method).
     */
    private <V> NameValue createNameValueAndAddToBiMapIfMissing(
            SafeBiMap<String, V> idToValue,V valueRepresentedByNameValue,
            Supplier<String> baseId, NameValueType nameValueType) {

        String id = idToValue.safeInverseGet(valueRepresentedByNameValue);
        if (id != null)
            return new NameValue(id);

        NameValue result = createNameValueWithUnusedId(baseId.get(), false, nameValueType);
        idToValue.putNewKey(result.id, valueRepresentedByNameValue);
        return result;
    }

    /**
     * @return A new {@link NameValue}, with an id identical to other {@link NameValue} objects representing the same
     * value if such were already created by this {@link NameValuesManager}.
     */
    public NameValue createNameValueRepresentingEnumValue(Enum<?> value) {
        return createNameValueAndAddToBiMapIfMissing(idToEnumValue, value, value::toString, ENUM);
    }

    public NameValue createNameValueRepresentingBoolean(boolean b) {
        return createNameValueRepresentingEnumValue(BooleanEnum.create(b));
    }

    /**
     * @return same documentation as for {@link #createNameValueRepresentingEnumValue(Enum)}.
     */
    public NameValue createNameValueRepresentingNliMethod(String functionFriendlyId) {
        return createNameValueAndAddToBiMapIfMissing(idToFunctionFriendlyId, functionFriendlyId,
                ()->functionFriendlyId, NLI_METHOD_NAME);
    }

    /**
     * @return same documentation as for {@link #createNameValueRepresentingEnumValue(Enum)}.
     */
    public NameValue createNameValueRepresentingNliEntity(State state, NliEntity entity) {
        String entityId = state.getEntityId(entity);
        return createNameValueAndAddToBiMapIfMissing(idToNliEntityId, entityId,
                () -> entity.getClass().getSimpleName() + "_" + entityId, NLI_ENTITY);
    }

    /**
     * @return same documentation as for {@link #createNameValueRepresentingEnumValue(Enum)}.
     */
    public NameValue createNameValueRepresentingNliEntityType(Class<? extends NliEntity> type) {
        return createNameValueAndAddToBiMapIfMissing(idToNliEntityType, type, type::getSimpleName, NLI_ENTITY_TYPE);
    }

    /**
     * @return same documentation as for {@link #createNameValueRepresentingEnumValue(Enum)}.
     */
    public NameValue createNameValueRepresentingRelationField(Field relation) {
        return createNameValueAndAddToBiMapIfMissing(idToFieldRelation, relation,
                () -> relation.getDeclaringClass().getSimpleName() + "." + relation.getName(), FIELD_RELATION);
    }

    /**
     * @return A new {@link NameValue} with an unused id.
     */
    public NameValue createNameValueRepresentingNonFieldRelation(String baseId, boolean useBaseIdAsId) {
        return createNameValueWithUnusedId(baseId, useBaseIdAsId, NON_FIELD_RELATION);
    }

    /**
     * @return same documentation as for {@link #createNameValueRepresentingEnumValue(Enum)}.
     */
    public NameValue createNameValueRepresentingString(String value) {
        return createNameValueAndAddToBiMapIfMissing(idToString, value, ()-> "\"" + value + "\"", STRING);
    }



    /**
     * @throws RuntimeException in case 'nameValue' does not represent an enum value.
     */
    public @NotNull Enum<?> getEnumValue(NameValue nameValue){
        return idToEnumValue.getExisting(nameValue.id);
    }

    /**
     * @throws RuntimeException in case 'nameValue' does not represent an NLI function.
     */
    public @NotNull String getNliMethodFriendlyId(NameValue nameValue){
        return idToFunctionFriendlyId.getExisting(nameValue.id);
    }

    /**
     * @throws RuntimeException in case 'nameValue' does not represent an {@link NliEntity}.
     */
    public @NotNull String getNliEntityId(NameValue nameValue){
        return idToNliEntityId.getExisting(nameValue.id);
    }

    /**
     * @throws RuntimeException in case 'nameValue' does not represent an {@link NliEntity} type.
     */
    public @NotNull Class<? extends NliEntity> getNliEntityType(NameValue nameValue){
        return idToNliEntityType.getExisting(nameValue.id);
    }

    /**
     * @throws RuntimeException in case 'nameValue' does not represent a field relation.
     */
    public @NotNull Field getRelationField(NameValue nameValue){
        return idToFieldRelation.getExisting(nameValue.id);
    }

    /**
     * @throws RuntimeException in case 'nameValue' does not represent a string value.
     */
    public @NotNull  String getString(NameValue nameValue){
        return idToString.getExisting(nameValue.id);
    }


    /**
     * @return null if 'nameValueWithReversibleId' is not managed by this object.
     */
    public @Nullable Boolean isNameValueRepresentBinaryRelation(NameValue nameValueWithReversibleId){
        NameValueType type = getNameValueType(nameValueWithReversibleId);
        if (type == null)
            return null;
        return type == NameValueType.FIELD_RELATION || type == NameValueType.NON_FIELD_RELATION;
    }

    /**
     * Note: throughout this module, whenever we say "relation" without specifying the arity, we mean a binary relation.
     * @return null if 'nameValueWithReversibleId' is not managed by this object.
     */
    public @Nullable Boolean isNameValueRepresentUnaryRelation(NameValue nameValueWithReversibleId){
        NameValueType type = getNameValueType(nameValueWithReversibleId);
        if (type == null)
            return null;
        //noinspection ConstantConditions
        return !isNameValueRepresentBinaryRelation(nameValueWithReversibleId);
    }

}
