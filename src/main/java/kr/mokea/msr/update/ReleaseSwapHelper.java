package kr.mokea.msr.update;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Runs in a separate JVM after the game exits, so the old JAR can be replaced on Windows. */
public final class ReleaseSwapHelper {
    private ReleaseSwapHelper() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 4) return;
        long parentPid = Long.parseLong(args[0]);
        Path oldJar = Path.of(args[1]);
        Path staged = Path.of(args[2]);
        Path target = Path.of(args[3]);
        for (int i = 0; i < 120 && ProcessHandle.of(parentPid).map(ProcessHandle::isAlive).orElse(false); i++) {
            Thread.sleep(1000);
        }
        if (ProcessHandle.of(parentPid).map(ProcessHandle::isAlive).orElse(false)) return;
        if (!Files.isRegularFile(staged)) return;
        Files.move(staged, target, StandardCopyOption.REPLACE_EXISTING);
        if (!oldJar.equals(target)) Files.deleteIfExists(oldJar);
    }
}
