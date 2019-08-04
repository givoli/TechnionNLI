package com.ofergivoli.ojavalib.io.log;

import com.google.common.base.Verify;
import com.ofergivoli.ojavalib.io.TextIO;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;

/**
 * Represents a logger that can log either to {@link Log} or to a specified writer.
 * In case logging to a writer, each write is immediately flushed.
 */
public class Logger {

    /**
     * When null, with use {@link Log} instead. Otherwise we use only the writer.
     */
    @Nullable
    private final Writer writer;

    /**
     * Used when logging to {@link Log}.
     */
    private Log.MessageType defaultLogMessageType = Log.MessageType.TRACE;


    public Logger(@SuppressWarnings("NullableProblems") Writer writer) {
        Verify.verify(writer != null);
        this.writer = writer;
    }



    /**
     * @param outLogFile Will be overwritten if already exists.
     */
    public Logger (File outLogFile, boolean createMissingDirectories) {
        if (createMissingDirectories) {
            File parent = outLogFile.getParentFile();
            if (!parent.exists()) {
                boolean ok = parent.mkdirs();
                Verify.verify(ok);
            }
        }
        this.writer = TextIO.getStandardStreamWriter(outLogFile);
    }

    /**
     * Will log to Log (default) until something is pushed to the stack.
     */
    public Logger(Log.MessageType messageType) {
        defaultLogMessageType = messageType;
        writer = null;
    }

    /**
     * Logs according to the top of the stack
     *  - Appends a newline to 'message'.
     *  - If writing to a writer - flushes.
     */
    public void log(String message) {
        if (writer == null)
            Log.log(defaultLogMessageType, message);
        else {
            try {
                writer.write(message);
                writer.write("\n");
                writer.flush();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }



}
