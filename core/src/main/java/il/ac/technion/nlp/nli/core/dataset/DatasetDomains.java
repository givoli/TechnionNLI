package il.ac.technion.nlp.nli.core.dataset;

import com.google.common.base.Verify;
import ofergivoli.olib.data_structures.map.SafeHashMap;
import ofergivoli.olib.data_structures.map.SafeMap;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * A collection of app domains.
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class DatasetDomains implements Serializable{


    private static final long serialVersionUID = 2112327921910916791L;


    private final SafeMap<String, Domain> domainIdToDomain = new SafeHashMap<>();
    private final SafeMap<String, Domain> rootEntityClassNameToDomain = new SafeHashMap<>();

    public @NotNull Domain getDomainById(String id) {
        Domain domain = domainIdToDomain.safeGet(id);
        Verify.verify(domain != null);
        return domain;
    }

    public List<Domain> getDomainsByIds(String... ids) {
        List<Domain> result = new LinkedList<>();
        for (String id : ids) {
            result.add(getDomainById(id));
        }
        return result;
    }

    public Domain getDomainByRootEntityClass(Class<? extends NliRootEntity> clazz) {
        Domain domain = rootEntityClassNameToDomain.safeGet(clazz.getCanonicalName());
        Verify.verify(domain != null);
        return domain;
    }

    @SuppressWarnings("SameParameterValue")
    public void addDomain(String domainId, Class<? extends NliRootEntity> rootEntityClass) {
        Domain domain = new Domain(domainId, rootEntityClass);
        Verify.verify(domainIdToDomain.put(domain.getId(), domain) == null);
        Verify.verify(rootEntityClassNameToDomain.put(rootEntityClass.getCanonicalName(), domain) == null);
    }


    public Collection<Domain> getDomains() {
        return domainIdToDomain.values();
    }

    @SafeVarargs
    public final List<Domain> getDomainsByRootEntityClasses(Class<? extends NliRootEntity>... rootEntityClasses) {
        List<Domain> result = new LinkedList<>();
        for (Class<? extends NliRootEntity> clazz : rootEntityClasses) {
            result.add(getDomainByRootEntityClass(clazz));
        }
        return result;
    }
}
