package ofergivoli.olib.general;

import com.google.common.base.Charsets;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Verify;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Support running commands using a bash shell (note: this includes Cygwin on Windows).
 */
public class CommandRunner {

    public final List<String> bashExeDefaultCandidates = Arrays.asList(
            "/bin/bash",
            "C:\\cygwin64\\bin\\bash.exe"
    );


    /**
     * {@link #bashExeDefaultCandidates} will be used to find the bash program's executable file in case used.
     */
    public CommandRunner(){
        this.bashExeSupplier = getDefaultShellExe();
    }

    public CommandRunner(Path bashExe){
        this.bashExeSupplier = Suppliers.memoize(()-> {
            if (!Files.isRegularFile(bashExe))
                throw new RuntimeException("The bash program's executable file  " + bashExe + " doesn't exist.");
            return bashExe;
        });
    }

    private final Supplier<Path> bashExeSupplier;

    /**
     * @throws RuntimeException in case no default bash program's executable file was found.
     */
    private Supplier<Path> getDefaultShellExe() {
        return Suppliers.memoize(()->{
            for (String pathStr : bashExeDefaultCandidates){
                Path path = Paths.get(pathStr);
                if (Files.isRegularFile(path))
                    return path;
            }
            throw new RuntimeException("Couldn't locate a bash program's executable file in the default locations. Candidates: " +
                    bashExeDefaultCandidates);
        });
    }


    public static class CommandExecutionResult {
        public int exitStatus;
        public String stdout;
        public String stderr;

        public CommandExecutionResult(int exitStatus, String stdout, String stderr) {
            this.exitStatus = exitStatus;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }


    public static class NonZeroExitStatus extends RuntimeException {
        private static final long serialVersionUID = 3840169338570180730L;

        public NonZeroExitStatus(String message) {
            super(message);
        }
    }




    /**
     * Runs the given commands, blocks until done, reading all of stdout & stderr (the reading from those two streams
     * is done from two new dedicated threads - so that this method won't block forever trying to read one of them while
     * the other streams's buffer is full and thus blocks the process running the command).
     *
     * @param expectExitStatusZero when true, in case of a non-zero exit status an exception is thrown.
     * @param useBashShell If true, the 'commandAndArgs' must consist of a single element. The command will be
     *                     pre-processed and executed by a bash shell (see constructor).
     *                     If false, no shell is used to pre-process 'commandAndArgs'.
     * @param loginShell Should be null if 'useBashShell' is false.
     *                   If true, the bash shell used is a login shell. The initialization of such a shell may take a
     *                   lot longer (but might be necessary for settings environment variables, etc.).
     * @param timeoutInMilliseconds If null then there is no timeout.
     *                              In case of a timeout, we throw {@link UncheckedTimeoutException} and kill the
     *                              suppress.
     *
     *                              Note about using ssh: in case of a timeout, if the command is an ssh command,
     *                              the process will keep running on the remote (even though an exception is thrown
     *                              by this method and the local ssh child process is killed) - at least that's a
     *                              behavior observed.
     * @throws NonZeroExitStatus in of a non-zero exit status, if 'expectExitStatusZero' is true.
     * @throws UncheckedTimeoutException in case of a timeout.
     */
    public CommandExecutionResult runAndBlock(boolean expectExitStatusZero, boolean useBashShell,
                                              @Nullable Boolean loginShell, @Nullable Long timeoutInMilliseconds,
                                              String... commandAndArgs) {
        Verify.verify((loginShell==null) == !useBashShell);
        if (useBashShell){
            if (commandAndArgs.length != 1) {
                throw new RuntimeException("when 'useBashShell' is true, commandAndArgs must consist of a single element. Instead it is: "
                        + Arrays.toString(commandAndArgs));
            }
            String[] newCommand = new String[3];
            newCommand[0] = bashExeSupplier.get().toString();
            newCommand[1] = loginShell ? "-lc" : "-c";
            newCommand[2] = commandAndArgs[0];
            commandAndArgs = newCommand;
        }

        Process process;
        try {
            process = Runtime.getRuntime().exec(commandAndArgs);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }


        // reading results & waiting:
        CommandExecutionResult result = new CommandExecutionResult(-999, null, null);
        Thread stdoutReader = new Thread(new SimpleReader(
                new InputStreamReader(process.getInputStream(), Charsets.UTF_8), stdout->result.stdout=stdout));
        stdoutReader.start();
        Thread stderrReader = new Thread(new SimpleReader(
                new InputStreamReader(process.getErrorStream(),Charsets.UTF_8), stderr->result.stderr=stderr));
        stderrReader.start();
        // waiting for the 2 reader threads:
        try {
            if (timeoutInMilliseconds == null) {
                process.waitFor();
            } else {
                boolean exitedSuccessfully = process.waitFor(timeoutInMilliseconds, TimeUnit.MILLISECONDS);
                if (!exitedSuccessfully) {
                    process.destroy();
                    throw new UncheckedTimeoutException("Timeout for command:\n" +
                            Arrays.toString(commandAndArgs));
                }
            }
            stdoutReader.join();
            stderrReader.join();
        } catch (InterruptedException e1) {
            throw new RuntimeException(e1);
        }


        // logic related to exit status:
        result.exitStatus = process.exitValue();
        if (result.exitStatus != 0 && expectExitStatusZero) {
            String errMsg = "The command:\n" + Arrays.toString(commandAndArgs)
                    + "\nexited with exit status " + result.exitStatus;
            errMsg += "\nstderr: " + result.stderr;
            errMsg += "\nstack trace: " + Arrays.toString(Thread.currentThread().getStackTrace());
            throw new CommandRunner.NonZeroExitStatus(errMsg);
        }

        return result;
    }


    /**
     * Reads all text from input stream and then sends it to given consumer.
     */
    private static class SimpleReader implements Runnable {

        private final BufferedReader input;
        private final Consumer<String> consumer;

        public SimpleReader(InputStreamReader input, Consumer<String> consumer) {
            this.input = new BufferedReader(input);
            this.consumer = consumer;
        }

        @Override
        public void run() {
            StringBuilder sb = new StringBuilder();
            String line;
            try {
                while ((line = input.readLine()) != null){
                    sb.append(line).append("\n");
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            consumer.accept(sb.toString());
        }
    }
    
}
