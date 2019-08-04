package il.ac.technion.nlp.nli.dataset1.domains.messenger.hit;

import com.ofergivoli.ojavalib.data_structures.set.SafeHashSet;
import com.ofergivoli.ojavalib.data_structures.set.SafeSet;
import com.ofergivoli.ojavalib.string.StringManager;
import il.ac.technion.nlp.nli.dataset1.domains.messenger.ChatGroup;
import il.ac.technion.nlp.nli.dataset1.domains.messenger.Messenger;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.core.dataset.visualization.StateVisualizer;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.HtmlString;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.TextColor;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.TextFormatModification;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.TextSizeModification;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class MessengerStateVisualizer extends StateVisualizer {


    private static final long serialVersionUID = 25434178258659860L;

    SafeSet<String> idsOfChatGroupsWithEmphasizedMutedMark = new SafeHashSet<>();

    /**
     * When not null, this contains the ids of all chat groups except those that where created by the function call, and
     * then the new ones will be emphasized.
     */
    @Nullable SafeSet<String> idsOfOldChatGroups;


    public void emphasizeMutedMark(String chatGroupId) {
        idsOfChatGroupsWithEmphasizedMutedMark.add(chatGroupId);
    }


    @Override
    public HtmlString getVisualRepresentation(State state) {

        Messenger messenger = (Messenger) state.getRootEntity();

        StringBuilder sb = new StringBuilder();

        sb.append("<div style=\"text-align: left;\">");

        TextFormatModification format = new TextFormatModification();
        format.setBold(true);
        format.setSizeInCssEm(TextSizeModification.BIGGER_XXX);
        format.setColor(TextColor.GREEN);
        String str = createHtmlFromStr("Chat Groups:", format);
        sb.append(str);

        sb.append("<table style=\"font-size: 2.0em;border-collapse:collapse;text-align:center;\">")
                .append("<tr style=\"background-color: #ccffff;\">" +
                        "<td style=\"border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">" +
                        "Contacts" +
                        "</span></td>" +
                        "<td style=\"border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">" +
                        "Participants #" +
                        "</span></td>" +
                        "<td style=\"border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">" +
                        "<div style=\"line-height:110%;\">Special<br>Settings</div>" +
                        "</span></td>" +
                        "</tr>");

        messenger.chatGroups.forEach(group->writeVisualRepresentationOfChatGroup(state, sb, group));

        sb.append("</table>");


        sb.append("</div>");
        return new HtmlString(sb.toString());
    }

    private void writeVisualRepresentationOfChatGroup(State s, StringBuilder sb, ChatGroup group) {

        TextFormatModification formatForEntireEvent = new TextFormatModification();
        String groupId = s.getEntityId(group);
        boolean allGroupEmphasized = isEntityEntirelyEmphasized(groupId) ||
                (idsOfOldChatGroups != null && !idsOfOldChatGroups.safeContains(groupId));
        if (allGroupEmphasized)
            formatForEntireEvent.setColor(TextColor.RED);

        List<String> firstNames = group.contacts.stream()
                .map(user -> user.firstName)
                .collect(Collectors.toList());


        String contactFirstNamesStr = createHtmlFromStr(StringManager.collectionToStringWithDelimiter(firstNames, ", "),
                formatForEntireEvent);
        String mutedMarkStr = "";
        if (group.muted) {
            TextFormatModification formatForMutedMark = allGroupEmphasized ?
                    formatForEntireEvent : getFormatForMutedMark(s, group);
            mutedMarkStr = createHtmlFromStr("* Muted *", formatForMutedMark);
        }
        String participatingNumStr = createHtmlFromStr(Integer.toString(group.participantsNumber), formatForEntireEvent);

        String str = "<tr>" +
                "<td style=\"border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">" +
                contactFirstNamesStr +
                "</span></td>" +
                "<td style=\"border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">" +
                participatingNumStr +
                "</span></td>" +
                "<td style=\"border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">" +
                mutedMarkStr +
                "</span></td>" +
                "</tr>";

        sb.append(str);
    }


    public TextFormatModification getFormatForMutedMark(State s, ChatGroup group) {
        TextFormatModification result = new TextFormatModification();
        if (idsOfChatGroupsWithEmphasizedMutedMark.safeContains(s.getEntityId(group)))
            result.setColor(TextColor.RED);
        return result;
    }
}
