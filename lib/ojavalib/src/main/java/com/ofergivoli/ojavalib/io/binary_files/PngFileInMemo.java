package com.ofergivoli.ojavalib.io.binary_files;

import java.io.File;

/**
 * Represents a png file, held in memory.
 * Immutable.
 */
public class PngFileInMemo extends BinFileInMemo {

    public PngFileInMemo(File inFile) {
        super(inFile, "png");
    }
}
