package com.ofergivoli.ojavalib.io.binary_files;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;


public class BinIO {

    public static byte[] readFile(File inFile) throws IOException {
        return Files.readAllBytes(Paths.get(inFile.getAbsolutePath()));
    }


    public static void writeFile(File outFile, byte[] fileData, boolean okToOverwrite) throws IOException {

        if (!okToOverwrite && outFile.exists())
        {
            throw new UncheckedIOException(new IOException("File already exists!"));
        }

        Files.write(Paths.get(outFile.getAbsolutePath()),fileData);
    }
}
