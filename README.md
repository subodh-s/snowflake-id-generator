
# Snowflake ID Generator

## Table of Contents
1. [Introduction](#introduction)
2. [Features](#features)
3. [Requirements](#requirements)
4. [Implementation](#implementation)
5. [Installation](#installation)
6. [Usage](#usage)
7. [Testing](#testing)
8. [Contributing](#contributing)
9. [License](#license)
10. [Questions or Feedback](#questions-or-feedback)

---

## Introduction
This repository contains a **Java** implementation of a **Snowflake ID Generator**, designed to produce **compact, globally unique, and sortable identifiers** for distributed systems. The IDs are 64-bit integers, making them highly efficient for storage and indexing.

---

## Features
- **Compact IDs**: Uses 64-bit integers for minimal storage overhead.
- **Global Uniqueness**: Ensures uniqueness across multiple instances.
- **Sortable IDs**: Based on timestamps, providing natural sorting by creation time.
- **High Throughput**: Capable of generating thousands of unique IDs per second.

---

## Requirements
- **Java 8** or higher (tested with Java 11 and above).
- (Optional) **Maven** for building and testing.

---

## Implementation

Below are the main source files included in this repository: the Snowflake ID generator class and its corresponding test class.

### `SnowflakeIdGenerator.java`

```java
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
            throw new IllegalArgumentException("Machine ID must be between 0 and " + MAX_MACHINE_ID);
        }
        this.machineId = machineId;
    }

    public synchronized long generateId() {
        long currentTimestamp = getCurrentTimestamp();

        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards. Refusing to generate ID.");
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                currentTimestamp = waitForNextMillisecond(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

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
}
```

### `SnowflakeIdGeneratorTest.java`

```java
package com.example.snowflake;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SnowflakeIdGeneratorTest {

    @Test
    void testGenerateIdsForHighThroughput() throws InterruptedException {
        final int numberOfRequests = 10_000;
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1);

        Set<Long> generatedIds = Collections.synchronizedSet(new HashSet<>());
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        for (int i = 0; i < numberOfRequests; i++) {
            executorService.submit(() -> {
                long id = generator.generateId();
                synchronized (generatedIds) {
                    assertFalse(generatedIds.contains(id), "Duplicate ID found!");
                    generatedIds.add(id);
                }
            });
        }

        executorService.shutdown();
        boolean finished = executorService.awaitTermination(10, TimeUnit.SECONDS);

        assertTrue(finished, "All requests did not complete in time");
        assertEquals(numberOfRequests, generatedIds.size(), "Mismatch in the number of generated IDs");
    }
}
```

---

## Installation

### 1. Clone or Download
```bash
git clone https://github.com/subodh-s/snowflake-id-generator.git
cd snowflake-id-generator
```

### 2. Build with Maven
```bash
mvn clean install
```

---

## Usage

### Generating IDs

```java
import com.example.snowflake.SnowflakeIdGenerator;

public class Main {
    public static void main(String[] args) {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1);
        long uniqueId = generator.generateId();
        System.out.println("Generated ID: " + uniqueId);
    }
}
```

**Output Example**
```
Generated ID: 2104638289113165824
```

---

## Testing

Run the included unit tests to verify functionality and throughput.

```bash
mvn test
```

---

## Contributing

1. Fork this repository.
2. Create a new branch (`git checkout -b feature-xyz`).
3. Commit your changes (`git commit -m "Add new feature"`).
4. Push to the branch (`git push origin feature-xyz`).
5. Submit a Pull Request.

---

## License

[MIT License](LICENSE)

---

## Questions or Feedback?

Open an [issue](../../issues) or reach out via Pull Requests.

Happy coding!
