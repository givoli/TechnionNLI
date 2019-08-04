package il.ac.technion.nlp.nli.dataset1.domains.file_manager.entities;

import il.ac.technion.nlp.nli.core.state.NliEntity;

import java.util.List;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class Directory  implements NliEntity {

    private static final long serialVersionUID = -8310454135063773718L;

    public String name;
    public List<File> childFiles;
    public List<Directory> childDirectories;

    public Directory(String name, List<File> childFiles, List<Directory> childDirectories) {
        this.name = name;
        this.childFiles = childFiles;
        this.childDirectories = childDirectories;
    }
}
