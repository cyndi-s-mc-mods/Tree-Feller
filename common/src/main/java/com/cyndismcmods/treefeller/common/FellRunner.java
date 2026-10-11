package com.cyndismcmods.treefeller.common;

import java.util.List;
import java.util.function.Consumer;

public final class FellRunner {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    private FellRunner() {}

    /** Breaks logs in order, damaging the tool once per log (none in creative). Returns logs broken. */
    public static int fell(List<BlockPos3> logs, Consumer<BlockPos3> breakBlock, ToolDamage tool, boolean creative) {
        int broken = 0;
        for (BlockPos3 log : logs) {
            breakBlock.accept(log);
            broken++;
            if (!creative && tool.damageOne()) {
                break;
            }
        }
        return broken;
    }

    /** Runs action with the re-entry guard set; the guard is always cleared afterwards. */
    public static void run(Runnable action) {
        ACTIVE.set(true);
        try {
            action.run();
        } finally {
            ACTIVE.set(false);
        }
    }

    public static boolean isActive() {
        return ACTIVE.get();
    }
}
