package io.agentengine.core.auth.handler;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SigningKeyHandlerTest {
    @TempDir
    Path directory;

    @Test
    void concurrentApplicationsShareTheSameSigningKey() throws Exception {
        // Dos instancias concurrentes crean y leen una única clave compartida sin
        // fallar por solapamiento.
        Path key = directory.resolve("keys/signing.json");
        var barrier = new CyclicBarrier(2);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                barrier.await();
                return new SigningKeyHandler().loadOrCreate(key.toString());
            });
            var second = executor.submit(() -> {
                barrier.await();
                return new SigningKeyHandler().loadOrCreate(key.toString());
            });
            assertEquals(first.get(15, TimeUnit.SECONDS).toJSONString(), second.get(15, TimeUnit.SECONDS).toJSONString());
        }
    }
}
