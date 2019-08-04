package il.ac.technion.nlp.nli.core.dataset.visualization.html;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * An HTML string that represents some data formatted nicely (to be shown to a human worker).
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class HtmlString implements Serializable {
	private static final long serialVersionUID = 8953171378485585571L;

	private final String html;

	public HtmlString(String html) {
		this.html = html;
	}

	
	/**
	 * @param str Text or HTML.
	 *            May be the returned value of {@link #getString()} (thereby nesting multiple
	 * 			  {@link TextFormatModification}).
	 * @param escapeCurrentHtml When true, we will escape anything that currently interprets as html language
	 * elements in 'str'. When false - we won't escape anything (note that html tags will be added as prefix
	 * and suffix of 'str').
	 */
	public HtmlString(String str, TextFormatModification format, boolean escapeCurrentHtml) {
		
		//TODO: test all features of format with this constructor.
		this(applyTextFormatModification(str, format, escapeCurrentHtml));
	}

	/**
	 * See: {@link #HtmlString(String, TextFormatModification, boolean)}
	 * @return html string.
	 */
	@NotNull
	private static String applyTextFormatModification(String str, TextFormatModification format, boolean escapeCurrentHtml) {
		if (escapeCurrentHtml)
			str = StringEscapeUtils.escapeHtml4(str);

		List<String> prefixes = new LinkedList<>();
		List<String> suffixes = new LinkedList<>(); // suffixes[i] is the suffix matching prefixes[i]


		// font size:
		if (format.getSizeModificationInCssEm() != null) {
            prefixes.add("<span style=\"font-size: " + format.getSizeModificationInCssEm() +"em;\">");
            suffixes.add("</span>");
        }

		// font color:
		if (format.getColor() != null) {
            prefixes.add("<span style=\"color: " + format.getColor().getHtmlColorCode() + ";\">");
            suffixes.add("</span>");
        }

		// bold
		if (format.getBold()) {
			prefixes.add("<b>");
			suffixes.add("</b>");
		}


		//underline
		if (format.getUnderline()) {
			prefixes.add("<span style=\"border-bottom: 0.13em solid;\">");
			suffixes.add("</span>");
		}


		// constructing the html string
		StringBuilder sb = new StringBuilder();
		prefixes.forEach(sb::append);

		sb.append(str);

		ReverseListIterator<String> ri = new ReverseListIterator<>(suffixes);
		while (ri.hasNext())
			sb.append(ri.next());


		return sb.toString();
	}

	public String getString() {
		return html;
	}

	
	@Override
	public String toString() {
		return getString();
	}
}
