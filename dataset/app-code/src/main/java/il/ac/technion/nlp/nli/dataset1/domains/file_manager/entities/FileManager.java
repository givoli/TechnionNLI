package il.ac.technion.nlp.nli.dataset1.domains.file_manager.entities;

import il.ac.technion.nlp.nli.core.EnableNli;
import il.ac.technion.nlp.nli.core.NliDescriptions;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;

import java.util.Collection;
import java.util.List;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class FileManager implements NliRootEntity {
    private static final long serialVersionUID = 6685796086494133973L;

    public List<File> allFiles; // TODO: consider removing
    public List<Directory> allDirectories; // TODO: consider removing

    public Directory cwd;

    public FileManager(List<File> allFiles, List<Directory> allDirectories, Directory cwd) {
        this.allFiles = allFiles;
        this.allDirectories = allDirectories;
        this.cwd = cwd;
    }

    @EnableNli
    @NliDescriptions(descriptions = {"remove", "delete"})
    public void removeFiles(Collection<File> files) {
        files.forEach(this::removeFile);
    }

    private void removeFile(File f) {
        removeFileUnderDir(cwd,f);
    }

    private void removeFileUnderDir(Directory d, File f) {
        if (!d.childFiles.remove(f))
            d.childDirectories.forEach(childDir->removeFileUnderDir(childDir, f));
    }

    @EnableNli
    @NliDescriptions(descriptions = {"move"})
    public void moveFiles(Collection<File> files, Directory targetDirectory) {
        files.stream()
                .sorted() // we sort in order to get deterministic results in our experiments.
                .forEach(f-> {
                    removeFile(f);
                    targetDirectory.childFiles.add(f);
                });
    }
}
