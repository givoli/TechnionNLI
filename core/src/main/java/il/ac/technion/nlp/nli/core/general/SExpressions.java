package il.ac.technion.nlp.nli.core.general;

import java.util.Collection;
import java.util.regex.Matcher;

/**
 * Auxiliary methods for manipulating strings that represent (LISP) S-Expressions.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class SExpressions {

    private static String TWO_BACKSLASHES = Matcher.quoteReplacement("\\\\");
    private static String BACKSLASH_LEFTBRACKET = Matcher.quoteReplacement("\\(");
    private static String BACKSLASH_RIGHTBRACKET = Matcher.quoteReplacement("\\)");
    private static String BACKSLASH_COMMA = Matcher.quoteReplacement("\\,");

    public static String escapeSExpressionSyntax(String str) { //TODO: TEST
        str = str.replaceAll("\\\\", TWO_BACKSLASHES);
        str = str.replaceAll("\\(", BACKSLASH_LEFTBRACKET);
        str = str.replaceAll("\\(", BACKSLASH_RIGHTBRACKET);
        str = str.replaceAll(",", BACKSLASH_COMMA);
        return str;
    }

    /**
     * @param sExpresions each element should either be an s-expression.
     * @return a new S-expression whose elements are the given S-expressions.
     */
    public static String createSExpressionFromChildSExpressions(Collection<String> sExpresions) {
        StringBuilder sb = new StringBuilder().append("(");
        boolean first = true;
        for (String sExp : sExpresions) {
            if (!first)
                sb.append(",");
            first = false;
            sb.append(sExp);
        }
        sb.append(")");
        return sb.toString();
    }


    public static String getHumanReadableString(String sExp) {
        StringBuilder sb = new StringBuilder();
        int indentCount = 0;
        boolean first = true;
        for (char c : sExp.toCharArray()) {
            if (c == '(') {
                indentCount++;
                auxFor__getHumanReadableString__startNewLine(sb, indentCount, first);
            } else if (c == ')')
                indentCount--;
            else if (c == ',')
                auxFor__getHumanReadableString__startNewLine(sb, indentCount, first);
            else
                sb.append(c);

            first = false;
        }
        return sb.toString();
    }


    private static void auxFor__getHumanReadableString__startNewLine(
            StringBuilder sb, int indentCount, boolean first) {

        if (!first)
            sb.append("\n");
        for (int i = 0; i < indentCount; i++)
            sb.append("\t");
    }
}

