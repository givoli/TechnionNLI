package ofergivoli.olib.io.files_tree;

import ofergivoli.olib.exceptions.UncheckedFileNotFoundException;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class FileTreeManager {


    /**
     * @return A list of all files and directories that are descendants of 'dir' (excluding 'dir'). Order: DFS pre-order.
     * @throws UncheckedFileNotFoundException If 'dir' is not an existing directory.
     */
    public static List<Path> getAllFilesAndDirectoriesUnderDir(Path dir){
        File dirFile = dir.toFile();
        if (!dirFile.isDirectory())
            throw new UncheckedFileNotFoundException();

        List<Path> result = new LinkedList<>();
        //noinspection ConstantConditions
        for (File f : dirFile.listFiles()) {
            result.add(f.toPath());
            if (f.isDirectory())
                result.addAll(getAllFilesAndDirectoriesUnderDir(f.toPath()));
        }

        return result;
    }


	/**
	 * 
	 * @param extension - excluding the dot. If this is null - there's no restriction on the extension.
	 * @return A list of all files (not directories) that are descendants of 'dir' and have the extension 
	 * 'extension'.
	 * @throws UncheckedFileNotFoundException If 'dir' is not an existing directory.
	 */
	public static List<Path> getAllRegularFilesUnderDirWithGivenExtension(Path dir, String extension) {

	    return getAllFilesAndDirectoriesUnderDir(dir).stream()
                .filter(f -> Files.isRegularFile(f) && (extension == null || f.getFileName().endsWith("." + extension)))
                .collect(Collectors.toList());
	}


	/**
	 * 
	 * @return A list of all files (not directories) that are descendants of 'dir'.
	 * @throws UncheckedFileNotFoundException If 'dir' is not an existing directory.
	 */
	public static List<Path> getAllRegularFilesUnderDir(Path dir) {
		return getAllRegularFilesUnderDirWithGivenExtension(dir, null);
	}

    /**
     * @return A list of all directories that are descendants of 'dir' (excluding 'dir').
     * @throws UncheckedFileNotFoundException If 'dir' is not an existing directory.
     */
    public static List<Path> getAllDirectoriesUnderDir(Path dir) {
        return getAllFilesAndDirectoriesUnderDir(dir).stream()
                .filter(f -> Files.isDirectory(f))
                .collect(Collectors.toList());
    }

	/**
	 * @return If clazz was loaded from a jar file, the directory of the jar file is returned.
     * If clazz was loaded from a class file, then the parent of the main classes directory is returned (the main
	 * classes directory is the one containing the packages file-tree).
     * Either way, if you're compiling the source of clazz as part of your project, you can expect that the
     * "target directory" will be returned (and thus the name of the method).
     */
	public static Path getTargetDir(Class<?> clazz) {
		String filename;
		try {
			filename = clazz.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
		} catch (URISyntaxException e) {
			throw new RuntimeException("class was loaded in a 'special' manner that this method does not support (1)");
		}
		Path p = Paths.get(filename);

		if (!Files.exists(p))
			throw new RuntimeException("class was loaded in a 'special' manner that this method does not support (2)");


		if (Files.isRegularFile(p)) {
			// p represents a jar file
			return p.getParent();
		}

        // p represents the main classes directory.
		return p.getParent();
	}

	public static File getTargetDirFile(Class<?> clazz) {
		return getTargetDir(clazz).toFile();
	}

		/**
         * @return all file (including directories) directly in 'directory'.
         */
	public static Collection<Path> getFilesInDirectory(Path directory){
		try {
			return Files.list(directory).collect(Collectors.toList());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
