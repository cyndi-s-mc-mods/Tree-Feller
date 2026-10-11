package com.cyndismcmods.treefeller.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class TreeScanner {
    static final int MAX_LOGS = 512;

    private TreeScanner() {
    }

    /**
     * Returns the logs of the tree containing origin, bottom-up, or an empty list
     * if origin is not a bottom log, the group has no natural leaf, or it exceeds MAX_LOGS.
     */
    static List<BlockPos3> findTree(TreeWorld world, BlockPos3 origin) {
        if (!world.isSameLog(origin) || world.isSameLog(origin.offset(0, -1, 0))) {
            return List.of();
        }

        Set<BlockPos3> seen = new HashSet<>();
        ArrayDeque<BlockPos3> queue = new ArrayDeque<>();
        List<BlockPos3> logs = new ArrayList<>();
        seen.add(origin);
        queue.add(origin);
        boolean hasLeaf = false;

        while (!queue.isEmpty()) {
            BlockPos3 current = queue.poll();
            logs.add(current);
            if (logs.size() > MAX_LOGS) {
                return List.of();
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos3 next = current.offset(dx, dy, dz);
                        if (world.isNaturalLeaf(next)) {
                            hasLeaf = true;
                        } else if (world.isSameLog(next) && seen.add(next)) {
                            queue.add(next);
                        }
                    }
                }
            }
        }

        if (!hasLeaf) {
            return List.of();
        }
        logs.sort(Comparator.comparingInt(BlockPos3::y)
                .thenComparingInt(BlockPos3::x)
                .thenComparingInt(BlockPos3::z));
        return logs;
    }
}
