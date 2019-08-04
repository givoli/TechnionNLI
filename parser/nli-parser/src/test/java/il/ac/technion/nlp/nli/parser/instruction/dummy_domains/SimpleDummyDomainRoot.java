package il.ac.technion.nlp.nli.parser.instruction.dummy_domains;

import il.ac.technion.nlp.nli.core.EnableNli;
import il.ac.technion.nlp.nli.core.dataset.Domain;
import il.ac.technion.nlp.nli.core.method_call.MethodId;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 * Static methods are auxiliary (not part of regular root entities logic).
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
@SuppressWarnings("unused")
public class SimpleDummyDomainRoot implements NliRootEntity {
    private static final long serialVersionUID = -3317405914599583097L;

    public static Domain domain = new Domain(SimpleDummyDomainRoot.class.getName(), SimpleDummyDomainRoot.class);

    public static MethodId getMethodIdOfDummyNliMethod(){
        return new MethodId(SimpleDummyDomainRoot.class, "dummyNliMethod",
                EnumUsedToDefineFieldInRoot.class, Collection.class);
    }


    EnumUsedToDefineFieldInRoot definedInRoot = EnumUsedToDefineFieldInRoot.USED;
    List<DummyEntity> dummyEntities = Collections.singletonList(new DummyEntity());

    @EnableNli
    public void dummyNliMethod(EnumUsedToDefineFieldInRoot arg1,
                               Collection<EnumUsed> arg2) {
    }

    public enum EnumUsed {
        UNUSED, USED_BUT_NOT_FOR_QUERYING, USED2
    }

    public enum EnumNotUsedForArg {
        A, B
    }

    /**
     * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
     */
    public static enum EnumUsedToDefineFieldInRoot {
        USED, UNUSED
    }
}
