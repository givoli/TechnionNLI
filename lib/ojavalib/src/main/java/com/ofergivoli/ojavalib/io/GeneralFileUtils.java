package com.ofergivoli.ojavalib.io;

import com.google.common.base.Verify;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class GeneralFileUtils {

    /**
     * Sometimes deleting a file fails because of another unrelated process is accessing the file (e.g. antivirus, I
     * suppose). This method keeps trying forever until it succeeds in deleting the file.
     * @param fileToDelete either a file or a directory.
     */
    public static void deleteAndKeepTryingUntilSuccessful(Path fileToDelete, int milisecToSleepAfterEachFailure) {

        while(true) {

            try {
                if (Files.isDirectory(fileToDelete))
                    FileUtils.deleteDirectory(fileToDelete.toFile());
                else
                   Files.delete(fileToDelete);
                return;
            } catch (IOException e) {
                try {
                    Thread.sleep(milisecToSleepAfterEachFailure);
                } catch (InterruptedException e2) {
                    throw new RuntimeException(e2);
                }
            }
        }
    }

    /**
     * Does not overwrite any files (throws exception if destFile already exist).
     * @param source - may be a directory.
     * @param destFile - Must not already exist.
     * @throws RuntimeException on failure.
     */
    public static void safeMove(Path source, Path destFile) {
        Verify.verify(!Files.exists(destFile));
        try {
            Files.move(source, destFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @param dir May exist already.
     */
    public static void mkdirs(File dir) {
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
    }

    public static void createDirectory(Path directory){
        try {
            java.nio.file.Files.createDirectory(directory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates missing directories along the path of 'directory'. If the directory already exists, nothing happens.
     */
    public static void createDirectories(Path directory) {
        try {
            java.nio.file.Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void copy(Path source, Path target) {
        try {
            Files.copy(source, target);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    /**
     * @return the home directory of the current user.
     */
    public static Path getHomeDir() {
        return Paths.get(System.getProperty("user.home"));
    }


}
