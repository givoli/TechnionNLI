package il.ac.technion.nlp.nli.parser.instruction.dummy_domains;

import il.ac.technion.nlp.nli.core.state.NliEntity;

import java.util.Collections;
import java.util.List;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
@SuppressWarnings("unused")
public class DummyEntity implements NliEntity {
    private static final long serialVersionUID = -3317405914599583097L;

    SimpleDummyDomainRoot.EnumUsed usedButNotForQuerying = SimpleDummyDomainRoot.EnumUsed.USED_BUT_NOT_FOR_QUERYING;
    SimpleDummyDomainRoot.EnumNotUsedForArg usedButNotForArg = SimpleDummyDomainRoot.EnumNotUsedForArg.A;
    List<SimpleDummyDomainRoot.EnumUsed> usedValuesList = Collections.singletonList(SimpleDummyDomainRoot.EnumUsed.USED2);
}
