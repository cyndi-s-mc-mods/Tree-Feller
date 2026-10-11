package com.cyndismcmods.treefeller.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TreeScannerTest {

    @Test
    void feltsConnectedTrunkWithCanopyBottomUp() {
        GridWorld w = new GridWorld();
        w.trunk(0, 0, 0, 4);
        w.leaf(0, 4, 0);
        assertEquals(4, TreeScanner.findTree(w, new BlockPos3(0, 0, 0)).size());
        assertEquals(0, TreeScanner.findTree(w, new BlockPos3(0, 0, 0)).get(0).y());
    }

    @Test
    void includesDiagonalLogsForTwoByTwoTrunk() {
        GridWorld w = new GridWorld();
        w.trunk(0, 0, 0, 3);
        w.trunk(1, 0, 0, 3);
        w.trunk(0, 0, 1, 3);
        w.trunk(1, 0, 1, 3);
        w.leaf(0, 3, 0);
        assertEquals(12, TreeScanner.findTree(w, new BlockPos3(0, 0, 0)).size());
    }

    @Test
    void pillarWithoutNaturalLeavesIsNotATree() {
        GridWorld w = new GridWorld();
        w.trunk(0, 0, 0, 5);
        assertTrue(TreeScanner.findTree(w, new BlockPos3(0, 0, 0)).isEmpty());
    }

    @Test
    void nonBottomLogIsNotATree() {
        GridWorld w = new GridWorld();
        w.trunk(0, 0, 0, 4);
        w.leaf(0, 4, 0);
        assertTrue(TreeScanner.findTree(w, new BlockPos3(0, 2, 0)).isEmpty());
    }

    @Test
    void overCapIsNotFelled() {
        GridWorld w = new GridWorld();
        w.trunk(0, 0, 0, TreeScanner.MAX_LOGS + 1);
        w.leaf(0, TreeScanner.MAX_LOGS + 1, 0);
        assertTrue(TreeScanner.findTree(w, new BlockPos3(0, 0, 0)).isEmpty());
    }

    @Test
    void differentWoodNextToTreeIsNotFelled() {
        GridWorld w = new GridWorld();
        w.trunk(0, 0, 0, 4);
        w.leaf(0, 4, 0);
        assertEquals(4, TreeScanner.findTree(w, new BlockPos3(0, 0, 0)).size());
        assertFalse(TreeScanner.findTree(w, new BlockPos3(0, 0, 0)).contains(new BlockPos3(1, 0, 0)));
    }
}
