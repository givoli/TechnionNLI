package il.ac.technion.nlp.nli.dataset1.domains.messenger.hit;

import ofergivoli.olib.data_structures.set.SafeHashSet;
import ofergivoli.olib.data_structures.set.SafeSet;
import il.ac.technion.nlp.nli.core.dataset.DatasetDomains;
import il.ac.technion.nlp.nli.core.dataset.construction.hit_random_generation.HitRandomGenerator;
import il.ac.technion.nlp.nli.core.dataset.visualization.StateVisualizer;
import il.ac.technion.nlp.nli.core.method_call.MethodCall;
import il.ac.technion.nlp.nli.core.method_call.NonPrimitiveArgument;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.dataset1.SettingsCommonForMultipleDomains;
import il.ac.technion.nlp.nli.dataset1.domains.messenger.ChatGroup;
import il.ac.technion.nlp.nli.dataset1.domains.messenger.Messenger;
import il.ac.technion.nlp.nli.dataset1.domains.messenger.User;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class MessengerHitRandomGenerator extends HitRandomGenerator {

    private final SettingsCommonForMultipleDomains settingsCommonForMultipleDomains;

    public MessengerHitRandomGenerator(Random rand, MessengerNliMethod messengerNliMethod,
                                       DatasetDomains datasetDomains,
                                       SettingsCommonForMultipleDomains settingsCommonForMultipleDomains) {

        super(datasetDomains.getDomainByRootEntityClass(Messenger.class), rand, MessengerStateVisualizer::new);
        this.messengerNliMethod = messengerNliMethod;
        this.settingsCommonForMultipleDomains = settingsCommonForMultipleDomains;
    }

    private final MessengerNliMethod messengerNliMethod;

    @Nullable
    @Override
    protected NliRootEntity generateRandomRootEntityForInitialState() {

        int MIN_CONTACTS_NUM = 5;
        int MAX_CONTACTS_NUM = 10;
        int MIN_CHAT_GROUPS_NUM = 2;
        int MAX_CHAT_GROUPS_NUM = 4;


        Messenger messenger = new Messenger();

        int contactsNum = sampleUniformly(MIN_CONTACTS_NUM, MAX_CONTACTS_NUM + 1);
        int chatGroupsNum = sampleUniformly(MIN_CHAT_GROUPS_NUM, MAX_CHAT_GROUPS_NUM + 1);

        messenger.contacts = generateRandomContactsWithUniqueFirstNames(contactsNum);
        for (int i = 0; i < chatGroupsNum; i++)
            messenger.chatGroups.add(createRandomChatGroup(messenger.contacts));

        return messenger;
    }

    private ChatGroup createRandomChatGroup(List<User> contacts) {

        int MIN_NUM_OF_PARTICIPANTS_IN_GROUP = 2; // but lower bound is sampled contacts number.
        int MAX_NUM_OF_PARTICIPANTS_IN_GROUP = 10;
        int MIN_NUM_OF_CONTACTS_IN_GROUP = 2;
        int MAX_NUM_OF_CONTACTS_IN_GROUP = 3;

        int groupContactsNum = sampleUniformly(MIN_NUM_OF_CONTACTS_IN_GROUP,
                Math.min(MAX_NUM_OF_CONTACTS_IN_GROUP, contacts.size()) + 1);
        boolean muted = sampleOverBernoulliDistribution(0.5);
        List<User> groupContacts = sampleSubsetUniformly(contacts, groupContactsNum);
        int participantsNum = Math.max(groupContactsNum,
                sampleUniformly(MIN_NUM_OF_PARTICIPANTS_IN_GROUP, MAX_NUM_OF_PARTICIPANTS_IN_GROUP + 1));
        return new ChatGroup(groupContacts, muted, participantsNum);
    }

    private List<User> generateRandomContactsWithUniqueFirstNames(int usersNum) {
        SafeSet<String> firstNames = new SafeHashSet<>();
        while (firstNames.size() < usersNum)
            firstNames.add(sampleOverFiniteGeometricDistribution(0.5, settingsCommonForMultipleDomains.someFirstNames));
        return firstNames.stream().map(User::new).collect(Collectors.toList());
    }


    @Nullable
    @Override
    protected MethodCall generateRandomFunctionCall(State initialState, StateVisualizer initialStateVisualizer,
                                                    StateVisualizer desiredStateVisualizer) {

        MessengerStateVisualizer initialSV = (MessengerStateVisualizer) initialStateVisualizer;
        MessengerStateVisualizer desiredSV = (MessengerStateVisualizer) desiredStateVisualizer;

        if (messengerNliMethod == MessengerNliMethod.CREATE_CHAT_GROUP)
            return generateRandomCreateChatGroupFunctionCall(initialState,  desiredSV);


        final int MIN_GROUPS_NUM_TO_AFFECT = 1;
        final int MAX_GROUPS_NUM_TO_AFFECT = 4;


        Messenger messenger = (Messenger) initialState.getRootEntity();

        int groupsNumToAffect = Math.min(messenger.chatGroups.size(),
                sampleUniformly(MIN_GROUPS_NUM_TO_AFFECT, MAX_GROUPS_NUM_TO_AFFECT+1));

        ArrayList<ChatGroup> groups = sampleSubsetUniformly(messenger.chatGroups, groupsNumToAffect);
        SafeSet<String> groupIds = groups.stream()
                .map(initialState::getEntityId)
                .collect(Collectors.toCollection(SafeHashSet::new));

        switch (messengerNliMethod) {
            case DELETE_CHAT_GROUP:
                groupIds.forEach(initialSV::emphasizeEntireEntity);
                break;
            case MUTE_CHAT_GROUP:
                groups.stream()
                        .filter(g->!g.muted)
                        .map(initialState::getEntityId)
                        .forEach(desiredSV::emphasizeMutedMark);
                break;
            case UNMUTE_CHAT_GROUP:
                groups.stream()
                        .filter(g->g.muted)
                        .map(initialState::getEntityId)
                        .forEach(initialSV::emphasizeMutedMark);
                break;
        }

        return new MethodCall(messengerNliMethod.getMethodId(), initialState.getEntityId(messenger),
                new NonPrimitiveArgument(groupIds));
    }


    private MethodCall generateRandomCreateChatGroupFunctionCall(
            State initialState, MessengerStateVisualizer desiredStateVisualizer) {

        final int MIN_CONTACTS_NUM = 2;
        final int MAX_CONTACTS_NUM = 3;

        Messenger messenger = (Messenger) initialState.getRootEntity();

        int contactsNum = Math.min(messenger.contacts.size(), sampleUniformly(MIN_CONTACTS_NUM, MAX_CONTACTS_NUM+1));

        SafeSet<String> contactIds = sampleSubsetUniformly(messenger.contacts, contactsNum).stream()
                .map(initialState::getEntityId)
                .collect(Collectors.toCollection(SafeHashSet::new));

        desiredStateVisualizer.idsOfOldChatGroups = messenger.chatGroups.stream()
                .map(initialState::getEntityId)
                .collect(Collectors.toCollection(SafeHashSet::new));

        return new MethodCall(messengerNliMethod.getMethodId(), initialState.getEntityId(messenger),
                new NonPrimitiveArgument(contactIds));

    }

}
