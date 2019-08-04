package il.ac.technion.nlp.nli.dataset1.domains.list.hit;

import il.ac.technion.nlp.nli.dataset1.domains.list.entities.ListElement;
import il.ac.technion.nlp.nli.dataset1.domains.list.entities.SpecialList;
import il.ac.technion.nlp.nli.core.method_call.MethodId;
import il.ac.technion.nlp.nli.core.dataset.NliMethod;

import java.util.Collection;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public enum ListNliMethod implements NliMethod {

    REMOVE, MOVE_TO_BEGINNING, MOVE_TO_END;

    @Override
    public MethodId getMethodId() {
        switch (this) {
            case REMOVE:
                return new MethodId(SpecialList.class, "remove", Collection.class);
            case MOVE_TO_BEGINNING:
                return new MethodId(SpecialList.class, "moveToBeginning", ListElement.class);
            case MOVE_TO_END:
                return new MethodId(SpecialList.class, "moveToEnd", ListElement.class);
        }
        throw new Error();
    }


}
