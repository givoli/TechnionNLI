package il.ac.technion.nlp.nli.dataset1.domains.list.hit;

import ofergivoli.olib.data_structures.set.SafeHashSet;
import ofergivoli.olib.data_structures.set.SafeSet;
import il.ac.technion.nlp.nli.dataset1.domains.list.entities.ListElement;
import il.ac.technion.nlp.nli.dataset1.domains.list.entities.SpecialList;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.core.dataset.visualization.StateVisualizer;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.HtmlString;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.TextColor;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.TextFormatModification;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class ListStateVisualizer extends StateVisualizer {

    private static final long serialVersionUID = 6869742433532348109L;

    private final SafeSet<Integer> numbersToEmphasize = new SafeHashSet<>();


    @Override
    public HtmlString getVisualRepresentation(State state) {

        SpecialList list = (SpecialList) state.getRootEntity();
        StringBuilder sb = new StringBuilder();

        TextFormatModification format = new TextFormatModification().setSizeInCssEm(1.4);
        sb.append("<div style=\"text-align: left;\"><br>");
        sb.append(createHtmlFromStr("The List:", format));
        // the purpose of the following is to increase the width of the otherwise very narrow figure.
        sb.append(createHtmlFromStr("<br>____________________________________________________________",
                format.deepCopy().setColor(TextColor.WHITE),false,false));
        sb.append("</div>");
        format.setSizeInCssEm(2.3);

        sb.append("<div style=\"text-align: center;\">");
        boolean first = true;
        for (ListElement element : list.getElements()){
            if (!first)
                sb.append(createHtmlFromStr("<br>", format,false,false));
            first = false;
            boolean emphasize = numbersToEmphasize.safeContains(element.value);
            sb.append(createHtmlFromStr(Integer.toString(element.value), format, emphasize, true));
        }
        sb.append("</div>");
        return new HtmlString(sb.toString());

    }

    public void emphasizeNumber(int number){
        numbersToEmphasize.add(number);
    }

}
