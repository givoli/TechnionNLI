package com.ofergivoli.ojavalib.io.binary_files;

import com.ofergivoli.ojavalib.exceptions.UncheckedInvalidArgumentException;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;

/**
 * Represents a binary file, held in memory (probably less suitable for very large files).
 * Immutable.
 *
 */
public class BinFileInMemo implements Serializable {

    /**
     * The actual data of the file.
     */
    private byte[] fileData;


    /**
     * @param inFile This file will be loaded into memory by this constructor.
     */
    public BinFileInMemo(File inFile) {
        init(inFile);
    }

    private void init(File inFile) {
        try {
            fileData = BinIO.readFile(inFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Like {@link #BinFileInMemo(File)} - but enforces the file extension to be a specific given extension (case
     * sensitive).
     * @param extension The expected file extension of 'inFile' (excluding the dot). May not contain a dot.
     */
    public BinFileInMemo(File inFile, String extension) {
        if (!extension.equals(FilenameUtils.getExtension(inFile.getAbsolutePath())))
            throw new UncheckedInvalidArgumentException();

        init(inFile);
    }


    /**
     * @param okToOverwrite When true, 'outFile' will be overwritten if already existing (if already existing and this
     *                      is false - a RuntimeException is thrown).
     */
    public void saveFileToDisk(File outFile, boolean okToOverwrite) {
        if (outFile.exists() && !okToOverwrite)
            throw new RuntimeException("File already exists!");
        try {
            BinIO.writeFile(outFile, fileData,okToOverwrite);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


}
