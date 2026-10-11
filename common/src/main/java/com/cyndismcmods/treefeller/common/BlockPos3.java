package com.cyndismcmods.treefeller.common;

public record BlockPos3(int x, int y, int z) {
    public BlockPos3 offset(int dx, int dy, int dz) {
        return new BlockPos3(x + dx, y + dy, z + dz);
    }
}
