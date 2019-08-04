package il.ac.technion.nlp.nli.parser;

/**
 * {@link NameValuesManager} uses the values of this enum to represent the user values 'true' and 'false'.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public enum BooleanEnum {
    FALSE(false),
    TRUE(true);

    public final boolean value;

    BooleanEnum(boolean value) {
        this.value = value;
    }

    public static BooleanEnum create(boolean value) {
        if (value)
            return TRUE;
        return FALSE;
    }
}
