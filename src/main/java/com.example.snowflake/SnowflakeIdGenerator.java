package com.example.snowflake;

public class SnowflakeIdGenerator {
    private static final long CUSTOM_EPOCH = 1672531200000L; // Example epoch: 2023-01-01T00:00:00Z

    private static final long MACHINE_ID_BITS = 10;
    private static final long SEQUENCE_BITS = 12;

    private static final long MAX_MACHINE_ID = (1L << MACHINE_ID_BITS) - 1;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;

    private final long machineId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long machineId) {
        if (machineId < 0 || machineId > MAX_MACHINE_ID) {
            throw new IllegalArgumentException(String.format("Machine ID must be between 0 and %d", MAX_MACHINE_ID));
        }
        this.machineId = machineId;
    }

    public synchronized long generateId() {
        long currentTimestamp = getCurrentTimestamp();

        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards. Refusing to generate ID.");
        }

        if (currentTimestamp == lastTimestamp) {
            // Increment the sequence within the same millisecond
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // Sequence overflow, wait for the next millisecond
                currentTimestamp = waitForNextMillisecond(lastTimestamp);
            }
        } else {
            // Reset sequence for a new millisecond
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        // Construct the ID: timestamp | machineId | sequence
        return ((currentTimestamp - CUSTOM_EPOCH) << TIMESTAMP_SHIFT)
                | (machineId << MACHINE_ID_SHIFT)
                | sequence;
    }

    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    private long waitForNextMillisecond(long lastTimestamp) {
        long currentTimestamp = getCurrentTimestamp();
        while (currentTimestamp <= lastTimestamp) {
            currentTimestamp = getCurrentTimestamp();
        }
        return currentTimestamp;
    }

    public static void main(String[] args) {
        // Example: Machine ID is derived from hostname hash (ensure uniqueness per instance)
        long machineId = Math.abs(getHostName().hashCode()) % (MAX_MACHINE_ID + 1);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(machineId);

        // Generate 5 IDs as an example
        for (int i = 0; i < 5; i++) {
            long id = generator.generateId();
            System.out.println("Generated ID: " + id);
        }
    }

    private static String getHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown-host";
        }
    }
}

