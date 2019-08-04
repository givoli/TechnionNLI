package il.ac.technion.nlp.nli.dataset1.domains.messenger.hit;

import il.ac.technion.nlp.nli.dataset1.domains.messenger.Messenger;
import il.ac.technion.nlp.nli.core.method_call.MethodId;
import il.ac.technion.nlp.nli.core.dataset.NliMethod;

import java.util.Collection;

/**
 * TODO: have all domains each have such a class.
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public enum MessengerNliMethod implements NliMethod{
    CREATE_CHAT_GROUP,
    DELETE_CHAT_GROUP,
    MUTE_CHAT_GROUP,
    UNMUTE_CHAT_GROUP;

    @Override
    public MethodId getMethodId() {
        switch (this) {
            case CREATE_CHAT_GROUP:
                return new MethodId(Messenger.class, "createChatGroup", Collection.class);
            case DELETE_CHAT_GROUP:
                return new MethodId(Messenger.class, "deleteChatGroups", Collection.class);
            case MUTE_CHAT_GROUP:
                return new MethodId(Messenger.class, "muteChatGroups", Collection.class);
            case UNMUTE_CHAT_GROUP:
                return new MethodId(Messenger.class, "unmuteChatGroups", Collection.class);
        }
        throw new Error();
    }
}
