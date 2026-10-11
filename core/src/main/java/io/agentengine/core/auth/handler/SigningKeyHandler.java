package io.agentengine.core.auth.handler;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.UUID;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class SigningKeyHandler {
    public RSAKey loadOrCreate(String location) throws Exception {
        var path = Path.of(location).toAbsolutePath();
        Files.createDirectories(path.getParent());
        Files.setPosixFilePermissions(path.getParent(), PosixFilePermissions.fromString("rwx------"));

        try (var lockChannel = java.nio.channels.FileChannel.open(path.getParent().resolve("signing.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE); var ignored = acquireLock(lockChannel)) {
            if (Files.exists(path)) {
                return RSAKey.parse(Files.readString(path));
            }

            var key = new RSAKeyGenerator(3072).keyID(UUID.randomUUID().toString()).generate();
            var temporary = Files.createTempFile(path.getParent(), "jwk-", ".tmp", PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            Files.writeString(temporary, key.toJSONString());
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE);
            return key;
        }
    }

    private java.nio.channels.FileLock acquireLock(java.nio.channels.FileChannel channel) throws java.io.IOException {
        var deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(30);

        while (System.nanoTime() < deadline) {
            try {
                var lock = channel.tryLock();

                if (Objects.nonNull(lock)) {
                    return lock;
                }
            } catch (java.nio.channels.OverlappingFileLockException ignored) {
                // Both WARs can open the shared key simultaneously inside the same JVM.
            }

            try {
                Thread.sleep(25);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new java.io.IOException("Signing key initialization interrupted", ex);
            }
        }

        throw new java.io.IOException("Signing key initialization lock timed out");
    }
}
