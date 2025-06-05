package il.ac.technion.nlp.nli.dataset1.domains.file_manager.hit;

import ofergivoli.olib.data_structures.set.SafeHashSet;
import ofergivoli.olib.data_structures.set.SafeSet;
import il.ac.technion.nlp.nli.core.method_call.MethodCall;
import il.ac.technion.nlp.nli.dataset1.domains.file_manager.entities.Directory;
import il.ac.technion.nlp.nli.dataset1.domains.file_manager.entities.File;
import il.ac.technion.nlp.nli.dataset1.domains.file_manager.entities.FileManager;
import il.ac.technion.nlp.nli.core.method_call.MethodId;
import il.ac.technion.nlp.nli.core.method_call.NonPrimitiveArgument;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.core.dataset.DatasetDomains;
import il.ac.technion.nlp.nli.core.dataset.construction.hit_random_generation.HitRandomGenerator;
import il.ac.technion.nlp.nli.core.dataset.visualization.StateVisualizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class FileManagerHitRandomGenerator extends HitRandomGenerator {


    private final TaskType taskType;
    public enum TaskType {
        REMOVE_FILE,
        MOVE_FILE
    }

    public FileManagerHitRandomGenerator(Random rand, TaskType taskType, DatasetDomains datasetDomains) {
        super(datasetDomains.getDomainByRootEntityClass(FileManager.class), rand, FileManagerStateVisualizer::new);
        this.taskType = taskType;
    }

    private final ArrayList<String> POSSIBLE_DIRECTORY_NAMES = HitRandomGenerator.createArrayList("Work", "Vacation", "College", "Fun");
    private final ArrayList<String> FILE_TYPES = HitRandomGenerator.createArrayList("Image File", "PDF Document", "Text File", "Word Document");
    private final ArrayList<String> FILE_NAME_PREFIXES = HitRandomGenerator.createArrayList("car", "laptop", "dog", "task", "sarah", "jake");

    /**
     * @return A {@link FileManager} showing a file tree in depth of 3: the CWD directory only contains directories
     * which contain only files.
     */
    @Nullable
    @Override
    protected FileManager generateRandomRootEntityForInitialState() {


        final String CWD_NAME = "cwd"; //not shown in visualization.
        final int MIN_SUB_DIRECTORY_NUM = 2;
        final int MAX_SUB_DIRECTORY_NUM = 2;



        List<Directory> subDirs = new LinkedList<>();

        int subDirNum = sampleUniformly(MIN_SUB_DIRECTORY_NUM, MAX_SUB_DIRECTORY_NUM+1);
        ArrayList<String> dirNames = sampleSubsetUniformly(POSSIBLE_DIRECTORY_NAMES, subDirNum);
        for (int i=0; i<subDirNum; i++)
            subDirs.add(createRandomDirWithFiles(dirNames.get(i)));

        Directory cwd = new Directory(CWD_NAME, new LinkedList<>(), subDirs);
        List<Directory> allDirs = new LinkedList<>(subDirs);
        allDirs.add(cwd);
        return new FileManager(getAllFilesUnderDir(cwd), allDirs, cwd);

    }

    @NotNull
    private String getRandomFileName() {
        return sampleUniformly(FILE_NAME_PREFIXES) + gerRandomDigitsForFileNameSuffix();
    }

    private Directory createRandomDirWithFiles(String directoryName) {
        final int MIN_FILE_NUM_IN_DIRECTORY = 1;
        final int MAX_FILE_NUM_IN_DIRECTORY = 5;

        int fileNum = sampleUniformly(MIN_FILE_NUM_IN_DIRECTORY, MAX_FILE_NUM_IN_DIRECTORY+1);
        List<File> files = new LinkedList<>();
        for (int i=0; i<fileNum; i++)
            files.add(new File(getRandomFileName(), sampleUniformly(FILE_TYPES), getRandomFileSize()));
        return new Directory(directoryName, files, new LinkedList<>());
    }

    /**
     * We want a different long suffixes for each HIT so that the workers won't copy-paste long file names (preferring
     * that "cheating" approach to the more natural way of phrase the instruction).
     */
    private String gerRandomDigitsForFileNameSuffix() {
        int SUFFIX_DIGITS_NUM = 4;
        StringBuilder res = new StringBuilder();
        while (res.length()<SUFFIX_DIGITS_NUM)
            res.append(Integer.toString(rand.nextInt(10)));
        return res.toString();
    }

    private int getRandomFileSize() {
        final int MIN_DIGITS_NUM = 1;
        final int MAX_DIFITS_NUM = 8;

        assert (MAX_DIFITS_NUM <= Math.log10(Integer.MAX_VALUE));

        long res = 0;
        int digitsNum = sampleUniformly(MIN_DIGITS_NUM, MAX_DIFITS_NUM+1);
        return rand.nextInt((int)Math.round(Math.pow(10,digitsNum)));
    }

    private List<File>  getAllFilesUnderDir(Directory dir) {
        List<File> res = new LinkedList<>();
        aux_getAllFilesUnderDir(dir, res);
        return res;
    }

    private void aux_getAllFilesUnderDir(Directory dir, List<File> listToAppendTo) {
        listToAppendTo.addAll(dir.childFiles);
        dir.childDirectories.forEach(d->aux_getAllFilesUnderDir(d, listToAppendTo));
    }


    @Nullable
    @Override
    protected MethodCall generateRandomFunctionCall(State initialState, StateVisualizer initialStateVisualizer,
                                                    StateVisualizer desiredStateVisualizer) {

        final int MIN_FILES_NUM_TO_AFFECT = 1;
        final int MAX_FILES_NUM_TO_AFFECT = 4;

        //TODO: maybe refactor out all the duplicated logic.

        FileManager fileManager = (FileManager) initialState.getRootEntity();

        MethodId nliMethod;
        switch(taskType) {
            case REMOVE_FILE:
                nliMethod = new MethodId(FileManager.class, "removeFiles", Collection.class);
                break;
            case MOVE_FILE:
                nliMethod = new MethodId(FileManager.class, "moveFiles", Collection.class, Directory.class);
                break;
            default:
                throw new RuntimeException();
        }


        List<File> allFiles = getAllFilesUnderDir(fileManager.cwd);

        int filesNumToAffect = Math.min(allFiles.size(),
                sampleUniformly(MIN_FILES_NUM_TO_AFFECT, MAX_FILES_NUM_TO_AFFECT+1));


        SafeSet<String> filesIds = sampleSubsetUniformly(allFiles, filesNumToAffect).stream()
                .map(initialState::getEntityId)
                .collect(Collectors.toCollection(SafeHashSet::new));


        Directory targetDir = null;
        if (taskType == TaskType.MOVE_FILE) {
            targetDir = sampleUniformly(new ArrayList<>(fileManager.cwd.childDirectories));

            // we don't want some of the files we emphasize to be already in the targer directory
            Directory finalTargetDir = targetDir; // just for Java's shenanigans.
            filesIds = filesIds.stream().filter(id->!finalTargetDir.childFiles.stream().map(initialState::getEntityId)
                    .collect(Collectors.toList()).contains(id))
                    .collect(Collectors.toCollection(SafeHashSet::new));
            if (filesIds.isEmpty())
                return null;

            filesIds.forEach(desiredStateVisualizer::emphasizeEntireEntity);
        }

        filesIds.forEach(initialStateVisualizer::emphasizeEntireEntity);

        if (taskType == TaskType.MOVE_FILE) {
            return new MethodCall(nliMethod, initialState.getEntityId(fileManager),
                    new NonPrimitiveArgument(filesIds), new NonPrimitiveArgument(initialState.getEntityId(targetDir)));
        }

        return new MethodCall(nliMethod, initialState.getEntityId(fileManager), new NonPrimitiveArgument(filesIds));
    }
}
