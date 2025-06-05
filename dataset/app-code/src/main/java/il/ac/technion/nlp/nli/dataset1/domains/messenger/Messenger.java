package il.ac.technion.nlp.nli.dataset1.domains.messenger;

import ofergivoli.olib.io.log.Log;
import il.ac.technion.nlp.nli.core.EnableNli;
import il.ac.technion.nlp.nli.core.NliDescriptions;
import il.ac.technion.nlp.nli.core.method_call.InvalidNliMethodInvocation;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a messenger client app used by a given user.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class Messenger implements NliRootEntity {

    private static final long serialVersionUID = 315964429742665861L;

    public List<ChatGroup> chatGroups = new LinkedList<>();
    public List<User> contacts = new LinkedList<>();

    @EnableNli
    @NliDescriptions(descriptions = {"create", "new"})
    public void createChatGroup(Collection<User> groupContacts) {
        if (!contacts.containsAll(groupContacts)){
            Log.warn("contact not in contacts list...");
            throw new InvalidNliMethodInvocation();
        }
        List<User> ordered = groupContacts.stream()
                .sorted() // we sort just so the experiment results will be deterministic.
                .collect(Collectors.toList());

        chatGroups.add(new ChatGroup(ordered, false, groupContacts.size()));
    }

    @EnableNli
    @NliDescriptions(descriptions = {"delete", "remove"})
    public void deleteChatGroups(Collection<ChatGroup> c) {
        if (!chatGroups.containsAll(c)){
            Log.warn("Chat group not in chat groups list...");
            throw new InvalidNliMethodInvocation();
        }
        chatGroups.removeAll(c);
    }

    @EnableNli
    @NliDescriptions(descriptions = {"mute", "silence"})
    public void muteChatGroups(Collection<ChatGroup> chatGroups) {
        setMutedStateOfGroups(chatGroups, true);
    }

    @EnableNli
    @NliDescriptions(descriptions = {"unmute"})
    public void unmuteChatGroups(Collection<ChatGroup> chatGroups) {
        setMutedStateOfGroups(chatGroups, false);
    }

    private void setMutedStateOfGroups(Collection<ChatGroup> chatGroups, boolean muted) {
        chatGroups.forEach(group-> {
            if ( !chatGroups.contains(group) ) {
                Log.warn("Group entity not in chat groups list...");
                throw new InvalidNliMethodInvocation();
            }
            group.muted = muted;
        });
    }



}
