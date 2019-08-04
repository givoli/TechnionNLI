package com.ofergivoli.ojavalib.string;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class StringManager {

	public static final String NEWLINE = "\n";

	/**
	 * All empty strings are omitted.
	 * The newline separator may be either '\n' or "\r\n". 
	 */
	public static List<String> splitStringIntoNonEmptyLines(String str) {
		return Arrays.stream(str.split("\\r?\\n"))
				.filter(s->!s.isEmpty())
				.collect(Collectors.toList());
	}

	/**
	 * @return A single string which is the result of combining all elenebts in 'lines', adding {@link #NEWLINE} after
	 * each one (including after the last one).
     */
	public static String combineLines(List<String> lines) {
		StringBuilder sb = new StringBuilder();
		lines.forEach(l->sb.append(l).append(NEWLINE));
		return sb.toString();
	}

    /**
     * @return A string with a newline after every element.
     */
	public static <T> String collectionToStringWithNewlines(Collection<T> collection) {
        StringBuilder sb = new StringBuilder();
        collection.forEach(e->sb.append(e + "\n"));
        return sb.toString();
    }

	/**
	 * @return A string containing the {@link Object#toString()} of elements in 'collections', with a delimiter between
	 * every two consecutive elements.
	 */
	public static <T> String collectionToStringWithDelimiter(Collection<T> collection, String delimiter) {
		StringBuilder sb = new StringBuilder();
		Iterator<T> it = collection.iterator();
		while (it.hasNext()) {
			sb.append(it.next());
			if (it.hasNext())
				sb.append(delimiter);
		}
		return sb.toString();
	}
}
