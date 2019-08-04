package il.ac.technion.nlp.nli.parser.denotation;

import il.ac.technion.nlp.nli.core.method_call.Argument;
import il.ac.technion.nlp.nli.core.method_call.MethodId;
import il.ac.technion.nlp.nli.core.method_call.PrimitiveArgument;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.parser.instruction.dummy_domains.SimpleDummyDomainRoot;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class DenotationUtilsTest {


    @Test
    public void createFunctionCallTest(){

        State state = new State(SimpleDummyDomainRoot.domain, new SimpleDummyDomainRoot(), true);
        MethodId methodId = SimpleDummyDomainRoot.getMethodIdOfDummyNliMethod();

        List<Argument> args;


        // valid arguments (empty set):
        args = Arrays.asList(
                new PrimitiveArgument(SimpleDummyDomainRoot.EnumUsedToDefineFieldInRoot.USED),
                new PrimitiveArgument());
        assertNotNull(DenotationUtils.createFunctionCall(state, methodId, args));

        // valid arguments:
        args = Arrays.asList(
                new PrimitiveArgument(SimpleDummyDomainRoot.EnumUsedToDefineFieldInRoot.USED),
                new PrimitiveArgument(SimpleDummyDomainRoot.EnumUsed.USED2,
                                      SimpleDummyDomainRoot.EnumUsed.USED_BUT_NOT_FOR_QUERYING));
        assertNotNull(DenotationUtils.createFunctionCall(state, methodId, args));

        args = Arrays.asList(
                new PrimitiveArgument(SimpleDummyDomainRoot.EnumUsed.USED2), // wrong argument type
                new PrimitiveArgument(SimpleDummyDomainRoot.EnumUsed.USED2,
                        SimpleDummyDomainRoot.EnumUsed.USED_BUT_NOT_FOR_QUERYING));
        assertNull(DenotationUtils.createFunctionCall(state, methodId, args));

        args = Arrays.asList(
                // this argument is not supposed to be a collection:
                new PrimitiveArgument(SimpleDummyDomainRoot.EnumUsedToDefineFieldInRoot.USED,
                        SimpleDummyDomainRoot.EnumUsedToDefineFieldInRoot.USED),
                new PrimitiveArgument(SimpleDummyDomainRoot.EnumUsed.USED2,
                        SimpleDummyDomainRoot.EnumUsed.USED_BUT_NOT_FOR_QUERYING));
        assertNull(DenotationUtils.createFunctionCall(state, methodId, args));

        // switching arguments order:
        args = Arrays.asList(
                new PrimitiveArgument(SimpleDummyDomainRoot.EnumUsed.USED2,
                        SimpleDummyDomainRoot.EnumUsed.USED_BUT_NOT_FOR_QUERYING),
                new PrimitiveArgument(SimpleDummyDomainRoot.EnumUsedToDefineFieldInRoot.USED));

        assertNull(DenotationUtils.createFunctionCall(state, methodId, args));

    }

}