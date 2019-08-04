package il.ac.technion.nlp.nli.core.dataset;

import com.ofergivoli.ojavalib.data_structures.map.SafeHashMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeMap;
import il.ac.technion.nlp.nli.core.EnableNli;
import il.ac.technion.nlp.nli.core.method_call.MethodId;
import il.ac.technion.nlp.nli.core.reflection.EntityGraphReflection;
import il.ac.technion.nlp.nli.core.reflection.NliMethodReflection;
import il.ac.technion.nlp.nli.core.state.NliEntity;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * Represent a domain (app) for the instruction task.
 */
public class Domain implements Serializable {

    private static final long serialVersionUID = 8477482992305387914L;
    private final String rootEntityClassName;
    private final String id; // equals() and hashCode() work by this. Should be human readable.

    // The following are transient in order to reduce dependency of persistent data on package naming, entity graph definition etc.
    transient @Nullable private Collection<Field> relationFields;
    transient @Nullable private Collection<Method> nliMethods;
    transient @Nullable private Collection<Class<? extends NliEntity>> nliEntityClasses;

    /**
     * see {@link #getFriendlyIdToMethodId()}
     */
    private final SafeMap<String,MethodId> friendlyIdToMethodId;


    /**
     * @return the methods representing the interface methods: the methods of all {@link NliEntity}s that are annotated with
     * {@link EnableNli} including inherited methods and regardless of access modifier.
     */
    public Collection<Method> getNliMethods() {
        if (nliMethods == null)
            nliMethods = NliMethodReflection.findAllNliMethods(getRootEntityClass());
        return nliMethods;
    }

    /**
     * A mapping from human-friendly ids to the {@link MethodId}s of the interface methods.
     * The friendly ids shortness is also good for memory savings.
     */
    public SafeMap<String, MethodId> getFriendlyIdToMethodId() {
        return friendlyIdToMethodId;
    }

    /**
     * Deterministic.
     */
    public Domain(String id, Class<? extends NliRootEntity> rootEntityClass)
    {
        this.id = id;
        /**
         * Note: We need to use here {@link Class#getName()} and not {@link Class#getCanonicalName()} because the latter
         * can't be used to get the class via {@link Class#forName(String)} in case it's a static inner class.
         */
        this.rootEntityClassName = rootEntityClass.getName();
        friendlyIdToMethodId = createFriendlyIdToMethodIdMap(getNliMethods().stream()
                //sorting to get deterministic functionality:
                .sorted(Comparator.comparing(method->new MethodId(method).getDeterministicUniqueString()))
                .collect(Collectors.toList()));
    }

    /**
     * See: {@link #getFriendlyIdToMethodId()}
     * Deterministic.
     */
    private static SafeMap<String, MethodId> createFriendlyIdToMethodIdMap(Collection<Method> nliMethods) {
        SafeMap<String,MethodId> map = new SafeHashMap<>();
        nliMethods.forEach(method-> {
            MethodId methodId = new MethodId(method);
            String friendly = method.getDeclaringClass().getSimpleName() + "." + methodId.getName();
            if (map.safeContainsKey(friendly)) {
                String friendlyUntaken;
                int counter = 2;
                do {
                    friendlyUntaken =  friendly + "_" + counter;
                    counter++;
                } while (map.safeContainsKey(friendlyUntaken));
                map.put(friendlyUntaken, methodId);
            } else
                map.put(friendly, methodId);
        });
        return map;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }

    /**
     * Based on {@link #id}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Domain domain = (Domain) o;
        return Objects.equals(id, domain.id);
    }

    /**
     * Based on {@link #id}.
     */
    @Override
    public int hashCode() {

        return Objects.hash(id);
    }

    public Class<? extends NliRootEntity> getRootEntityClass() {
        try {
            //noinspection unchecked
            return (Class<? extends NliRootEntity>) Class.forName(rootEntityClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<Class<? extends NliEntity>> getNliEntityClasses() {
        if (nliEntityClasses == null)
            nliEntityClasses = EntityGraphReflection.getPossiblyReachableNliEntityClasses(getRootEntityClass(), true,
                    false);

        return nliEntityClasses;
    }

    public Collection<Field> getRelationFields() {
        if (relationFields == null)
            relationFields = findRelationFields();
        return relationFields;
    }

    private Collection<Field> findRelationFields() {
        return getNliEntityClasses().stream()
            .flatMap(clazz-> EntityGraphReflection.getRelationFieldsOfNliEntityClass(clazz, false).stream())
            .collect(Collectors.toList());
    }


}
