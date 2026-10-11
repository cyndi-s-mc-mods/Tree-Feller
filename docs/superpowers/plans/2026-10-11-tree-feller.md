# Tree-Feller Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Minecraft 26.3 mod for Fabric, Forge, and NeoForge that fells a whole tree when its bottom log is broken with an axe.

**Architecture:** A root Gradle project with `common`, `fabric`, `forge`, and `neoforge` subprojects. `common` is plain Java with no Minecraft dependency: it finds the tree, runs the per-log break and durability loop, and holds the re-entry guard. Each loader module adds a small adapter that reads the Minecraft world and tool, cancels the vanilla break, and calls `common`.

**Tech Stack:** Java 25, Gradle (wrapper from the Fabric 26.3 example), JUnit 5, Fabric Loom 1.18-SNAPSHOT, ModDevGradle 2.0.148 (NeoForge and Forge), Fabric API 0.162.0+26.3, NeoForge 26.3.0.69-beta.

**Spec:** `docs/superpowers/specs/2026-10-11-tree-feller-design.md`

## Global Constraints

- Minecraft version: `26.3`.
- Java toolchain: `25` for all subprojects.
- Fabric: Loader `0.19.5`, Loom `1.18-SNAPSHOT`, Fabric API `0.162.0+26.3`.
- NeoForge: `26.3.0.69-beta`, ModDevGradle `2.0.148`.
- Forge: `66.0.8` for 26.3 (build plugin set in Task 5).
- `mod_id=treefeller`, `mod_group=com.cyndismcmods.treefeller`, `mod_version=1.0.0`.
- Base package for code: `com.cyndismcmods.treefeller`.
- No config file, no dependency on other mods, no run-time dependency on Architectury or any helper library.
- Log cap: `TreeScanner.MAX_LOGS = 512`.

## Review Focus

- **Pillar touching a real tree:** a player's log column that touches natural leaves is felled with the tree. Accepted risk from the spec; no test prevents it.
- **Axe breaks partway through:** logs after the one that breaks the axe stay in place, and no further durability is taken. Pinned by Task 3.
- **Creative mode:** the tree is still felled, and durability is not taken. Pinned by Task 3.
- **Different wood next to the trunk:** a neighboring log of another type is not felled. Pinned by Task 2.
- **Not a tree:** a lone log, or a log column with no natural leaves, breaks normally and nothing else breaks. Pinned by Task 2.

---

### Task 1: Project scaffolding

**Files:**
- Create: `settings.gradle.kts`, `gradle.properties`, `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`, `.gitignore`
- Create: `common/build.gradle.kts`, `fabric/`, `forge/`, `neoforge/` (empty directories with `build.gradle.kts` placeholders)

**Interfaces:**
- Consumes: nothing.
- Produces: subprojects `:common`, `:fabric`, `:forge`, `:neoforge`. Later tasks add `:common` source and the loader builds.

- [ ] **Step 1: Copy the Gradle wrapper from the Fabric 26.3 example**

Copy `gradlew`, `gradlew.bat`, `gradle/wrapper/*` from `FabricMC/fabric-example-mod` branch `26.3`. Do not hand-write the wrapper.

- [ ] **Step 2: Write `gradle.properties`**

Use the Fabric example's `org.gradle.*` lines plus the mod properties:

```properties
org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true
org.gradle.configuration-cache=true
minecraft_version=26.3
mod_id=treefeller
mod_name=Tree Feller
mod_version=1.0.0
mod_group=com.cyndismcmods.treefeller
mod_license=MIT
loader_version=0.19.5
loom_version=1.18-SNAPSHOT
fabric_api_version=0.162.0+26.3
neo_version=26.3.0.69-beta
```

- [ ] **Step 3: Write `settings.gradle.kts`**

Include the four subprojects, and set the plugin repositories to Gradle Plugin Portal, Fabric Maven, NeoForged Maven, and MinecraftForge Maven. Add the foojay toolchain resolver so Java 25 downloads automatically.

- [ ] **Step 4: Verify**

Run: `./gradlew projects`
Expected: lists `:common`, `:fabric`, `:forge`, `:neoforge`. No build errors.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Scaffold multiloader Gradle project"
```

---

### Task 2: Tree detection in `common`

**Files:**
- Create: `common/src/main/java/com/cyndismcmods/treefeller/common/BlockPos3.java`
- Create: `common/src/main/java/com/cyndismcmods/treefeller/common/TreeWorld.java`
- Create: `common/src/main/java/com/cyndismcmods/treefeller/common/TreeScanner.java`
- Test: `common/src/test/java/com/cyndismcmods/treefeller/common/GridWorld.java`
- Test: `common/src/test/java/com/cyndismcmods/treefeller/common/TreeScannerTest.java`
- Modify: `common/build.gradle.kts` (add JUnit 5 test dependency and `useJUnitPlatform()`)

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `record BlockPos3(int x, int y, int z)` with `BlockPos3 offset(int dx, int dy, int dz)`.
  - `interface TreeWorld { boolean isSameLog(BlockPos3 pos); boolean isNaturalLeaf(BlockPos3 pos); }`. `isSameLog` means same wood type as the origin log. `isNaturalLeaf` means a leaf block with `persistent=false`.
  - `final class TreeScanner { static final int MAX_LOGS = 512; static List<BlockPos3> findTree(TreeWorld world, BlockPos3 origin); }`. Returns logs ordered bottom-up, with origin included. Returns an empty list if origin is not a bottom log, the group has no natural leaf, or the group exceeds `MAX_LOGS`.

- [ ] **Step 1: Write the failing tests**

`GridWorld` is a test helper implementing `TreeWorld` over a `Set<BlockPos3>` of logs and a `Set<BlockPos3>` of natural leaves. Its `isSameLog` checks the log set.

```java
@Test void feltsConnectedTrunkWithCanopyBottomUp() {
    GridWorld w = new GridWorld();
    w.trunk(0, 0, 0, 4);             // logs at y=0..3
    w.leaf(0, 4, 0);                 // natural leaf touching top log
    assertEquals(4, TreeScanner.findTree(w, new BlockPos3(0, 0, 0)).size());
    assertEquals(0, TreeScanner.findTree(w, new BlockPos3(0, 0, 0)).get(0).y());
}

@Test void includesDiagonalLogsForTwoByTwoTrunk() {
    GridWorld w = new GridWorld();
    w.trunk(0, 0, 0, 3); w.trunk(1, 0, 0, 3); w.trunk(0, 0, 1, 3); w.trunk(1, 0, 1, 3);
    w.leaf(0, 3, 0);
    assertEquals(12, TreeScanner.findTree(w, new BlockPos3(0, 0, 0)).size());
}

@Test void pillarWithoutNaturalLeavesIsNotATree() {
    GridWorld w = new GridWorld();
    w.trunk(0, 0, 0, 5);
    assertTrue(TreeScanner.findTree(w, new BlockPos3(0, 0, 0)).isEmpty());
}

@Test void nonBottomLogIsNotATree() {
    GridWorld w = new GridWorld();
    w.trunk(0, 0, 0, 4); w.leaf(0, 4, 0);
    assertTrue(TreeScanner.findTree(w, new BlockPos3(0, 2, 0)).isEmpty());
}

@Test void overCapIsNotFelled() {
    GridWorld w = new GridWorld();
    w.trunk(0, 0, 0, TreeScanner.MAX_LOGS + 1); w.leaf(0, TreeScanner.MAX_LOGS + 1, 0);
    assertTrue(TreeScanner.findTree(w, new BlockPos3(0, 0, 0)).isEmpty());
}
```

```java
@Test void differentWoodNextToTreeIsNotFelled() {
    GridWorld w = new GridWorld();
    w.trunk(0, 0, 0, 4); w.leaf(0, 4, 0);
    w.otherWood(1, 0, 0);            // a log of a different type beside the trunk
    assertEquals(4, TreeScanner.findTree(w, new BlockPos3(0, 0, 0)).size());
    assertFalse(TreeScanner.findTree(w, new BlockPos3(0, 0, 0)).contains(new BlockPos3(1, 0, 0)));
}
```

`GridWorld.otherWood` adds a log that `isSameLog` rejects.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :common:test`
Expected: FAIL, `TreeScanner` does not exist.

- [ ] **Step 3: Implement `TreeScanner`**

Breadth-first search over the 26 neighbors of each position, accepting only positions where `isSameLog` is true. Then:
1. Return empty if `origin.offset(0, -1, 0)` is a same-type log (not a bottom log).
2. Stop and return empty if the search exceeds `MAX_LOGS`.
3. Return empty unless at least one log has a `isNaturalLeaf` position among its 26 neighbors.
4. Sort the result by `y`, then `x`, then `z`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :common:test`
Expected: PASS, all 6 tests.

- [ ] **Step 5: Commit**

```bash
git add common
git commit -m "Add tree detection to common"
```

---

### Task 3: Break loop, durability, and re-entry guard in `common`

**Files:**
- Create: `common/src/main/java/com/cyndismcmods/treefeller/common/ToolDamage.java`
- Create: `common/src/main/java/com/cyndismcmods/treefeller/common/FellRunner.java`
- Test: `common/src/test/java/com/cyndismcmods/treefeller/common/FellRunnerTest.java`

**Interfaces:**
- Consumes: `BlockPos3` (Task 2).
- Produces:
  - `interface ToolDamage { boolean damageOne(); }` Applies one point of durability (Unbreaking applied by the loader). Returns `true` if the tool broke.
  - `final class FellRunner`:
    - `static int fell(List<BlockPos3> logs, Consumer<BlockPos3> breakBlock, ToolDamage tool, boolean creative)`. For each log in order: call `breakBlock`. If not creative, call `tool.damageOne()` and stop if it returns `true`. Returns the number of logs broken.
    - `static void run(Runnable action)`. Sets the guard for the duration of `action`, clears it in `finally`.
    - `static boolean isActive()`. True while inside `run`.

- [ ] **Step 1: Write the failing tests**

```java
@Test void breaksEveryLogAndDamagesOncePerLog() {
    List<BlockPos3> logs = List.of(new BlockPos3(0, 0, 0), new BlockPos3(0, 1, 0));
    List<BlockPos3> broken = new ArrayList<>();
    AtomicInteger damage = new AtomicInteger();
    ToolDamage tool = () -> { damage.incrementAndGet(); return false; };
    assertEquals(2, FellRunner.fell(logs, broken::add, tool, false));
    assertEquals(logs, broken);
    assertEquals(2, damage.get());
}

@Test void stopsWhenToolBreaksAndLeavesRemainingLogs() {
    // tool breaks on the second damage call: logs 0 and 1 are broken, log 2 stays
    assertEquals(2, FellRunner.fell(logs3, broken::add, breakAfter(2), false));
    assertEquals(2, broken.size());
}

@Test void creativeBreaksAllLogsWithoutDamage() {
    assertEquals(3, FellRunner.fell(logs3, b -> {}, () -> { fail("no damage in creative"); return false; }, true));
}

@Test void guardIsSetInsideRunAndClearedAfterEvenOnException() {
    assertFalse(FellRunner.isActive());
    assertThrows(IllegalStateException.class, () -> FellRunner.run(() -> {
        assertTrue(FellRunner.isActive());
        throw new IllegalStateException();
    }));
    assertFalse(FellRunner.isActive());
}
```

`breakAfter(n)` returns a `ToolDamage` that returns `true` on its nth call. Write it as a small helper in the test class.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :common:test`
Expected: FAIL, `FellRunner` does not exist.

- [ ] **Step 3: Implement `FellRunner`**

Use a `ThreadLocal<Boolean>` for the guard. `run` sets it to `true` in a `try` and resets it to `false` in `finally`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :common:test`
Expected: PASS, all tests from Tasks 2 and 3.

- [ ] **Step 5: Commit**

```bash
git add common
git commit -m "Add break loop, durability, and re-entry guard"
```

---

### Task 4: Fabric module

**Files:**
- Create: `fabric/build.gradle.kts`, `fabric/src/main/resources/fabric.mod.json`
- Create: `fabric/src/main/java/com/cyndismcmods/treefeller/fabric/TreeFellerFabric.java`
- Create: `fabric/src/main/java/com/cyndismcmods/treefeller/fabric/LevelTreeWorld.java`

**Interfaces:**
- Consumes: `TreeScanner.findTree`, `FellRunner.fell`, `FellRunner.run`, `FellRunner.isActive`, `ToolDamage` (Tasks 2 and 3).
- Produces: a mod entrypoint `TreeFellerFabric implements ModInitializer` registered in `fabric.mod.json`.

Verify all Minecraft and Fabric API names against the 26.3 Fabric example before use. The names below come from the Fabric and Minecraft conventions and are not yet compiled.

- [ ] **Step 1: Build script and metadata**

Copy the Fabric example's `build.gradle` setup, using `loom_version` and `fabric_api_version` from `gradle.properties`. Depend on `project(":common")` with `include`. Set `fabric.mod.json` `depends` to `fabricloader` and `fabric-api`. Set the entrypoint to `TreeFellerFabric`.

- [ ] **Step 2: Write `LevelTreeWorld`**

Implements `TreeWorld` over a `ServerLevel` and the origin's `Block`:
- `isSameLog(pos)`: `level.getBlockState(pos).is(origin)`.
- `isNaturalLeaf(pos)`: `state.is(BlockTags.LEAVES) && !state.getValue(LeavesBlock.PERSISTENT)`.
- Convert `BlockPos3` to `BlockPos` with `new BlockPos(x, y, z)`.

- [ ] **Step 3: Write `TreeFellerFabric`**

Register `PlayerBlockBreakEvents.BEFORE`. The handler:
1. Returns `true` (allow vanilla break) unless: the player is on the server, `!FellRunner.isActive()`, the held stack `is(ItemTags.AXES)`, and the state `is(BlockTags.LOGS)`.
2. Calls `TreeScanner.findTree(new LevelTreeWorld(...), new BlockPos3(pos))`. If empty, returns `true`.
3. Otherwise runs `FellRunner.run(...)` with:
   - `breakBlock`: `level.destroyBlock(pos, true, player)`.
   - `ToolDamage`: `stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND)`, returning `true` if the stack is empty afterward. Confirm the exact method and break check against the 26.3 sources.
   - `creative`: `player.getAbilities().instabuild`.
4. Returns `false` to cancel the vanilla break, since `FellRunner` already broke the origin.

- [ ] **Step 4: Verify the build**

Run: `./gradlew :fabric:build`
Expected: BUILD SUCCESSFUL, jar in `fabric/build/libs/`.

- [ ] **Step 5: Commit**

```bash
git add fabric
git commit -m "Add Fabric entrypoint and block-break hook"
```

---

### Task 5: Forge module

**Files:**
- Create: `forge/build.gradle.kts`, `forge/src/main/resources/META-INF/mods.toml`
- Create: `forge/src/main/java/com/cyndismcmods/treefeller/forge/TreeFellerForge.java`
- Create: `forge/src/main/java/com/cyndismcmods/treefeller/forge/LevelTreeWorld.java`

**Interfaces:**
- Consumes: the same `common` API as Task 4.
- Produces: a Forge mod entrypoint annotated `@Mod("treefeller")`.

- [ ] **Step 1: Confirm the Forge 26.3 build plugin**

Open the official Forge 26.3 MDK for version `66.0.8` at `files.minecraftforge.net` and copy its plugin and build setup. Do not assume ModDevGradle or ForgeGradle without checking the MDK. Record the plugin name and version in `forge/build.gradle.kts` as a comment.

- [ ] **Step 2: Write `LevelTreeWorld`**

Same logic as Task 4, using Forge's `Level` and `BlockState` APIs. Use `BlockTags.LOGS` and `BlockTags.LEAVES`, and `LeavesBlock.PERSISTENT`.

- [ ] **Step 3: Write `TreeFellerForge`**

Subscribe to `BlockEvent.BreakEvent` on the Forge event bus. Use the same gating and fell logic as Task 4. Cancel the vanilla break with `event.setCanceled(true)`.

- [ ] **Step 4: Verify the build**

Run: `./gradlew :forge:build`
Expected: BUILD SUCCESSFUL, jar in `forge/build/libs/`.

- [ ] **Step 5: Commit**

```bash
git add forge
git commit -m "Add Forge entrypoint and block-break hook"
```

---

### Task 6: NeoForge module

**Files:**
- Create: `neoforge/build.gradle.kts`, `neoforge/src/main/resources/META-INF/neoforge.mods.toml`
- Create: `neoforge/src/main/java/com/cyndismcmods/treefeller/neoforge/TreeFellerNeoForge.java`
- Create: `neoforge/src/main/java/com/cyndismcmods/treefeller/neoforge/LevelTreeWorld.java`

**Interfaces:**
- Consumes: the same `common` API as Task 4.
- Produces: a NeoForge mod entrypoint annotated `@Mod("treefeller")`.

- [ ] **Step 1: Build script**

Copy the setup from `NeoForgeMDKs/MDK-26.3-ModDevGradle`, using `neo_version` from `gradle.properties`. Depend on `project(":common")`. Set the Java toolchain to 25.

- [ ] **Step 2: Write `LevelTreeWorld` and `TreeFellerNeoForge`**

Same logic as Task 5. Subscribe to `BlockEvent.BreakEvent` and call `event.setCanceled(true)` after the tree is felled.

- [ ] **Step 3: Verify the build**

Run: `./gradlew :neoforge:build`
Expected: BUILD SUCCESSFUL, jar in `neoforge/build/libs/`.

- [ ] **Step 4: Commit**

```bash
git add neoforge
git commit -m "Add NeoForge entrypoint and block-break hook"
```

---

### Task 7: In-game verification

**Files:** none changed unless a check fails.

- [ ] **Step 1: Build all three jars**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL for `:common`, `:fabric`, `:forge`, `:neoforge`.

- [ ] **Step 2: Run each loader's client**

Run `./gradlew :fabric:runClient`, `:forge:runClient`, `:neoforge:runClient` in turn. In each one, check:
1. A natural oak tree with a canopy: break the bottom log with an axe, sneaking and not sneaking. The whole tree falls, leaves stay.
2. A 2x2 spruce: all four trunks fall.
3. A player-built log pillar with no leaves: only the broken block breaks.
4. Creative mode: the tree falls and the axe's durability does not change.
5. Survival with a low-durability axe: felling stops when the axe breaks, and the logs above stay.
6. Breaking a log with a non-axe tool: no felling.

- [ ] **Step 3: Report results**

Record which checks passed on which loader. Anything not run in-game is listed as unverified.
