package com.cyndismcmods.treefeller.common;

import java.util.HashSet;
import java.util.Set;

/** Test helper: a tree world backed by sets of positions. */
final class GridWorld implements TreeWorld {
    private final Set<BlockPos3> logs = new HashSet<>();
    private final Set<BlockPos3> leaves = new HashSet<>();

    /** Adds {@code height} logs stacked upward from (x, y, z). */
    void trunk(int x, int y, int z, int height) {
        for (int i = 0; i < height; i++) {
            logs.add(new BlockPos3(x, y + i, z));
        }
    }

    void leaf(int x, int y, int z) {
        leaves.add(new BlockPos3(x, y, z));
    }

    @Override
    public boolean isSameLog(BlockPos3 pos) {
        return logs.contains(pos);
    }

    @Override
    public boolean isNaturalLeaf(BlockPos3 pos) {
        return leaves.contains(pos);
    }
}
