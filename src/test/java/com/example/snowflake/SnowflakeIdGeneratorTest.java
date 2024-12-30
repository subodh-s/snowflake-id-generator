package com.example.snowflake;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SnowflakeIdGeneratorTest {

    @Test
    void testGenerateIdsForHighThroughput() throws InterruptedException {
        final int requestsPerSecond = 10000;
        final long machineId = 1; // Example machine ID
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(machineId);

        // Use a thread-safe set to collect generated IDs
        Set<Long> generatedIds = new HashSet<>();

        // Thread pool to simulate high concurrent requests
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        for (int i = 0; i < requestsPerSecond; i++) {
            executorService.submit(() -> {
                synchronized (generatedIds) {
                    long id = generator.generateId();
                    assertFalse(generatedIds.contains(id), "Duplicate ID found!");
                    generatedIds.add(id);
                }
            });
        }

        // Wait for all tasks to complete
        executorService.shutdown();
        boolean completed = executorService.awaitTermination(10, TimeUnit.SECONDS);

        // Ensure all IDs are generated and test passes
        assertTrue(completed, "All requests did not complete in time");
        assertEquals(requestsPerSecond, generatedIds.size(), "Mismatch in the number of generated IDs");
    }
}

