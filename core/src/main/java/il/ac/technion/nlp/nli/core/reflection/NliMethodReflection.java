package il.ac.technion.nlp.nli.core.reflection;

import com.google.common.base.Verify;
import com.ofergivoli.ojavalib.io.log.Log;
import com.ofergivoli.ojavalib.reflection.ReflectionUtils;
import il.ac.technion.nlp.nli.core.dataset.Domain;
import il.ac.technion.nlp.nli.core.EnableNli;
import il.ac.technion.nlp.nli.core.dataset.DatasetDomains;
import il.ac.technion.nlp.nli.core.state.NliEntity;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class NliMethodReflection {
    /**
     * Time complexity: O(1).
     */
    public static boolean isValidNliMethod(Method method) {
        if(!NliEntity.class.isAssignableFrom(method.getDeclaringClass()))
            return false;
        if(!method.isAnnotationPresent(EnableNli.class))
            return false;
        Type[] paramTypes = method.getGenericParameterTypes();
        for (Type type : paramTypes)
            if (!GeneralReflection.isUserEntityTypeOrCollectionThereof(type))
                return false;

        if (method.getReturnType().equals(Void.class))
            Log.warn("Return type of interface method is not void. The return value is currently ignored.");
        return true;
    }


    /**
     * See: {@link Domain#getNliMethods()}
     * @return all NLI methods which are methods of classes of entity that might be reachable from the
     * 'rootEntityClass'.
     */
    public static Collection<Method> findAllNliMethods(Class<? extends NliRootEntity> rootEntityClass) {

        Collection<Method> siMethods = new LinkedList<>();

        EntityGraphReflection.getPossiblyReachableNliEntityClasses(rootEntityClass, true, false)
                .forEach(nonPrimitiveEntityClass->
                    ReflectionUtils.getAllMethodsOfClass(nonPrimitiveEntityClass, false).stream()
                    .filter(method->method.isAnnotationPresent(EnableNli.class))
                    .forEach(method->{
                        Verify.verify(isValidNliMethod(method));
                        siMethods.add(method);
                    }));
        return siMethods;
    }


    public static boolean isMethodAnNLIMethodInOneOfDatasetDomains(DatasetDomains datasetDomains, Method method) {
        return datasetDomains.getDomains().stream().anyMatch(domain->domain.getNliMethods().contains(method));
    }

}
