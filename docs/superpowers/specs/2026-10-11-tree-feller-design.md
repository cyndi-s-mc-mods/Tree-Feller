# Tree-Feller Design Spec

## Goal

A Minecraft Java Edition mod for 26.3 (Java 25 bytecode) that fells a whole tree when the player breaks its bottom log with an axe. Supports Fabric, Forge, and NeoForge from one codebase.

## Behavior

### Trigger
- Breaking a log with an axe fells the tree, sneaking or not.
- The broken log must be the bottom log: the block directly below is not the same log type.
- Creative mode fells the tree without using durability.

### Tree detection
- Breadth-first search collects every connected log of the same type. Neighbors include diagonals, so 2x2 trunks (dark oak, spruce, jungle) are included.
- The group is a tree only if at least one of its logs touches a natural leaf block: a block in `#minecraft:leaves` with `persistent=false`. Player-placed leaves are persistent, so a player's log pillar with no natural canopy is left alone.
- Log cap: 512 logs per tree. Larger groups are not felled.

### Durability
- Each log removed costs the axe one durability, with Unbreaking applied per cost, as if each log had been broken by hand.
- If the axe breaks partway through, felling stops and the remaining logs stay.

### Breaking and drops
- Logs are removed bottom-up; each drops its normal item.
- Leaves are not touched and decay naturally.
- Logs are removed through the same break path as a normal break. A re-entry guard stops the mod's own breaks from triggering another fell.

## Code layout

- `common/`: BFS, tree validation, durability logic, block removal. Most of the code.
- `fabric/`, `forge/`, `neoforge/`: one small file each that subscribes to the platform's block-break event and calls into `common/`.
- No config file. No dependencies on other mods.

## Testing

- Unit tests for BFS and the canopy check, using a small in-memory block grid.
- Manual in-game checklist: natural oak, 2x2 spruce, player pillar, pillar next to a tree, creative mode, low-durability axe.

## Open items

- Gradle plugin versions for each loader are chosen during planning, checked against the 26.3 templates.
- Known risk: a player-built pillar with leaves touching it can be felled with a tree.
