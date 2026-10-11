package com.cyndismcmods.treefeller.common;

public interface TreeWorld {
    /** True if the block at pos is a log of the same wood type as the origin log. */
    boolean isSameLog(BlockPos3 pos);

    /** True if the block at pos is a leaf block with persistent=false. */
    boolean isNaturalLeaf(BlockPos3 pos);
}
