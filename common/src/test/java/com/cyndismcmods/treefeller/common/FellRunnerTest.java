package com.cyndismcmods.treefeller.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FellRunnerTest {

    private static ToolDamage breakAfter(int n) {
        AtomicInteger calls = new AtomicInteger();
        return () -> calls.incrementAndGet() == n;
    }

    @Test
    void breaksEveryLogAndDamagesOncePerLog() {
        List<BlockPos3> logs = List.of(new BlockPos3(0, 0, 0), new BlockPos3(0, 1, 0));
        List<BlockPos3> broken = new ArrayList<>();
        AtomicInteger damage = new AtomicInteger();
        ToolDamage tool = () -> { damage.incrementAndGet(); return false; };
        assertEquals(2, FellRunner.fell(logs, broken::add, tool, false));
        assertEquals(logs, broken);
        assertEquals(2, damage.get());
    }

    @Test
    void stopsWhenToolBreaksAndLeavesRemainingLogs() {
        List<BlockPos3> logs = List.of(new BlockPos3(0, 0, 0), new BlockPos3(0, 1, 0), new BlockPos3(0, 2, 0));
        List<BlockPos3> broken = new ArrayList<>();
        // tool breaks on the second damage call: logs 0 and 1 are broken, log 2 stays
        assertEquals(2, FellRunner.fell(logs, broken::add, breakAfter(2), false));
        assertEquals(2, broken.size());
    }

    @Test
    void creativeBreaksAllLogsWithoutDamage() {
        List<BlockPos3> logs = List.of(new BlockPos3(0, 0, 0), new BlockPos3(0, 1, 0), new BlockPos3(0, 2, 0));
        assertEquals(3, FellRunner.fell(logs, b -> {}, () -> { fail("no damage in creative"); return false; }, true));
    }

    @Test
    void guardIsSetInsideRunAndClearedAfterEvenOnException() {
        assertFalse(FellRunner.isActive());
        assertThrows(IllegalStateException.class, () -> FellRunner.run(() -> {
            assertTrue(FellRunner.isActive());
            throw new IllegalStateException();
        }));
        assertFalse(FellRunner.isActive());
    }
}
