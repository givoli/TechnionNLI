package il.ac.technion.nlp.nli.parser.lexicon;

import edu.stanford.nlp.sempre.*;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * NOTICE: the code of this class was copied from Percy Liang's {@link DateFn}, and was modified.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class TimeSemanticFn extends SemanticFn {

    public DerivationStream call(final Example ex, final SemanticFn.Callable c) {
        return new SingleDerivationStreamThatAddsToPhraseAssociation(ex.languageInfo.phrase(c.getStart(), c.getEnd())) {
            @Override
            public Derivation createDerivation() {
                String value = ex.languageInfo.getNormalizedNerSpan("TIME", c.getStart(), c.getEnd());
                if (value == null)
                    return null;
                TimeValue timeValue = parseTimeValueFromLanguageInfoFormat(value);
                if (timeValue == null)
                    return null;
                return new Derivation.Builder()
                        .withCallable(c)
                        .formula(new ValueFormula<>(timeValue))
                        .type(SemType.timeType)
                        .createDerivation();
            }
        };
    }

    /**
     * @param s The only parsable format of this argument is HH:MM preceded by "T". So for example: "T06:01" or
     *          "T23:59".
     * @return null in case failed to parse.
     */
    static @Nullable TimeValue parseTimeValueFromLanguageInfoFormat(String s) {
        if (!s.substring(0,1).equals("T"))
            return null;

        Matcher matcher = Pattern.compile("^T([0-9][0-9]):([0-9][0-9])$").matcher(s);
        if (!matcher.find())
            return null;

        int hour = Integer.parseInt(matcher.group(1));
        int minute = Integer.parseInt(matcher.group(2));

        if ((hour < 0 || hour > 23 || minute < 0 || minute > 59) && !(hour == 24 && minute == 0))
            return null;

        return new TimeValue(hour, minute);
    }

}
