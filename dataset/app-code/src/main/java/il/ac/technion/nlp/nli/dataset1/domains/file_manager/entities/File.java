package il.ac.technion.nlp.nli.dataset1.domains.file_manager.entities;

import il.ac.technion.nlp.nli.core.state.NliEntity;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class File implements NliEntity, Comparable {

    private static final long serialVersionUID = -134267645119977682L;

    public String name;
    public String type;
    public int sizeInBytes;

    public File(String name, String type, int sizeInBytes) {
        this.name = name;
        this.type = type;
        this.sizeInBytes = sizeInBytes;
    }

    /**
     * Note: this method exists to support sorting in interface functions, in order to get deterministic functionality.
     */
    @Override
    public int compareTo(@NotNull Object file) {
        Triple<String,String,Integer> t1 = new ImmutableTriple<>(name,type,sizeInBytes);
        File  f = (File) file;
        Triple<String,String,Integer> t2 = new ImmutableTriple<>(f.name,f.type,f.sizeInBytes);
        return t1.compareTo(t2);
    }
}
