package com.cyndismcmods.treefeller.common;

/** Applies one point of tool durability. Returns true if the tool broke. */
public interface ToolDamage {
    boolean damageOne();
}
