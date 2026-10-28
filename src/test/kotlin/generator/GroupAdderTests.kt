package generator

import Group.Companion.group
import dsltypes.init.TriggInit
import dsltypes.init.space.labelSpace
import editor.objects.SimpleObject
import editor.objects.data.Pos
import editor.objects.triggers.misc.SpawnTrigger
import editor.objects.triggers.misc.TimewarpTrigger
import editor.rawstring.property.getOrThrow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import provider.TriggProvider
import provider.spawn
import kotlin.random.Random
import kotlin.random.nextUInt

@OptIn(ExperimentalUnsignedTypes::class)
private class GroupAdderTests {
    @AfterEach
    fun afterEach() {
        TriggProvider.INSTANCE = null
    }

    @Test
    fun addTest() {
        val objAdder = GroupAdder(TriggProvider(), 5u)
        val obj = objAdder.add { SimpleObject(1u, Pos.ZERO) }

        Assertions.assertEquals(1, objAdder.collectedObjects.size)
        Assertions.assertEquals(obj, objAdder.collectedObjects.last())

        val spawnTrigger = objAdder.add { SpawnTrigger(Pos.ZERO) }
        Assertions.assertEquals(2, objAdder.collectedObjects.size)
        Assertions.assertEquals(spawnTrigger, objAdder.collectedObjects.last())
        Assertions.assertTrue(spawnTrigger.spawnTriggered.getOrThrow())
        Assertions.assertTrue(spawnTrigger.multiTriggered.getOrThrow())
    }

    @Test
    @DisplayName("Without calling spawn()")
    fun noSpawnAdder() {
        val objAdder = GroupAdder(TriggProvider(), 5u)
        objAdder.add { SimpleObject(5u, Pos.ZERO) }
        objAdder.add { TimewarpTrigger(Pos.ZERO, 1.5f) }

        Assertions.assertTrue(objAdder.collectedObjects.all {
            it.groups.getOrThrow().contains(5u)
                    && it.groups.getOrThrow().contains(TriggInit.DEFAULT.commonGroup.value)
                    && it.groups.size() == 2
        })
    }

    @Test
    @DisplayName("With calling spawn()")
    fun withSpawnAdder() {
        val objAdder = GroupAdder(TriggProvider(), 5u)
        objAdder.add { SimpleObject(5u, Pos.ZERO) }
        objAdder.add { TimewarpTrigger(Pos.ZERO, 1.5f) }

        objAdder.spawn {
            objAdder.add { SimpleObject(5u, Pos.ZERO) }
        }

        Assertions.assertFalse {
            objAdder.collectedObjects.all {
                it.groups.getOrThrow().contains(5u)
                        && it.groups.getOrThrow().contains(TriggInit.DEFAULT.commonGroup.value)
                        && it.groups.size() == 1
            }
        }

        Assertions.assertTrue {
            objAdder.spawnBranches.flatten().all {
                // 6u is the acquired group for the spawn trigger
                it.groups.getOrThrow().contains(6u)
                        && it.groups.getOrThrow().contains(TriggInit.DEFAULT.commonGroup.value)
                        && it.groups.size() == 2
            }
        }
    }

    @Test
    @DisplayName("Without adding to ctx")
    fun withoutAddingToContext() {
        val ctx = TriggProvider()
        val objAdder = GroupAdder(ctx, 5u, addObjectsToContext = false)
        objAdder.add { SimpleObject(5u, Pos.ZERO) }
        objAdder.add { TimewarpTrigger(Pos.ZERO, 1.5f) }

        objAdder.spawn {
            objAdder.add { SimpleObject(5u, Pos.ZERO) }
        }

        Assertions.assertTrue(ctx.addedObjects.isEmpty(), "ctx.addedObjects is not empty")
    }

    @Test
    @DisplayName("Spawn(group) test")
    fun spawnGroupTest() {
        val objAdder = GroupAdder(TriggProvider(), 5u)
        val spawnTrigger = objAdder.spawn(6.group)

        Assertions.assertEquals(1, objAdder.collectedObjects.size)
        Assertions.assertEquals(objAdder.collectedObjects.last(), spawnTrigger)

        Assertions.assertEquals(6u, spawnTrigger.targetGroup.getOrThrow())
        Assertions.assertEquals(0f, spawnTrigger.delay.getOrThrow())
        Assertions.assertEquals(5u, spawnTrigger.groups.getOrThrow().first.toUInt())


        val spawnTrigger2 = objAdder.spawn(9.group, delay = 7f)

        Assertions.assertEquals(2, objAdder.collectedObjects.size)
        Assertions.assertEquals(objAdder.collectedObjects.last(), spawnTrigger2)

        Assertions.assertEquals(9u, spawnTrigger2.targetGroup.getOrThrow())
        Assertions.assertEquals(7f, spawnTrigger2.delay.getOrThrow())
        Assertions.assertEquals(5u, spawnTrigger2.groups.getOrThrow().first.toUInt())
    }

    @Test
    @DisplayName("Spawn(group, lambda) test")
    fun spawnGroupLambdaTest() {
        val objAdder = GroupAdder(TriggProvider(), 5u)
        objAdder.add { SimpleObject(5u, Pos.ZERO) }
        objAdder.add { TimewarpTrigger(Pos.ZERO, 1.5f) }

        objAdder.spawn(91.group) {
            objAdder.add { SimpleObject(5u, Pos.ZERO) }
        }

        Assertions.assertFalse {
            objAdder.collectedObjects.all {
                it.groups.getOrThrow().contains(5u)
                        && it.groups.getOrThrow().contains(TriggInit.DEFAULT.commonGroup.value)
                        && it.groups.size() == 1
            }
        }

        Assertions.assertTrue {
            objAdder.spawnBranches.flatten().all {
                it.groups.getOrThrow().contains(91u)
                        && it.groups.getOrThrow().contains(TriggInit.DEFAULT.commonGroup.value)
                        && it.groups.size() == 2
            }
        }
    }

    @Test
    @DisplayName("ObjectManager.group test")
    fun groupTest() {
        val ctx = TriggProvider()
        val groupAdder = ctx.group(5u) {
            add { SimpleObject(5u, Pos.ZERO) }
            add { TimewarpTrigger(Pos.ZERO, 1.65f) }
        }

        Assertions.assertTrue {
            groupAdder.collectedObjects
                .all { it.groups.getOrThrow().contains(5u) && it.groups.getOrThrow().contains(ctx.init.commonGroup.value) }
        }
        Assertions.assertEquals(2, groupAdder.collectedObjects.size)


        val ctx2 = TriggProvider()
        val groupAdder2 = ctx2.group(5u) {
            add { SimpleObject(5u, Pos.ZERO) }
            add { TimewarpTrigger(Pos.ZERO, 1.65f) }

            spawn {
                add { SimpleObject(5u, Pos.ZERO) }
                add { TimewarpTrigger(Pos.ZERO, 1.65f) }
            }
        }

        Assertions.assertFalse {
            groupAdder2.collectedObjects
                .all { it.groups.getOrThrow().contains(5u) && it.groups.getOrThrow().contains(ctx.init.commonGroup.value) }
        }

        Assertions.assertEquals(
            2 + 1, // 2 added objects + 1 implicit spawn trigger
            groupAdder2.collectedObjects
                .filter { it.groups.getOrThrow().contains(5u) }
                .size
        )
        Assertions.assertEquals(
            2,
            groupAdder2.collectedObjects
                // 1u would be the acquired group for the spawn branch
                .filter { it.groups.getOrThrow().contains(1u) }
                .size
        )
    }

    @Test
    @DisplayName("ObjectManager.group test (nested)")
    fun nestedGroupTest() {
        val ctx = TriggProvider()
        val groupAdder = ctx.group(5u) {
            add { SimpleObject(2u, Pos.ZERO) }
            group(6u) {
                add { SimpleObject(5u, Pos.ZERO) }
                add { TimewarpTrigger(Pos.ZERO, 1.65f) }
            }
        }

        Assertions.assertFalse {
            groupAdder.collectedObjects
                .all { it.groups.getOrThrow().contains(5u) && it.groups.getOrThrow().contains(6u) }
        }
        Assertions.assertTrue {
            groupAdder.collectedObjects
                .all { it.groups.getOrThrow().contains(5u) && it.groups.getOrThrow().contains(ctx.init.commonGroup.value) }
        }

        Assertions.assertEquals(
            2,
            groupAdder.collectedObjects
                // 1u would be the acquired group for the spawn branch
                .filter { it.groups.getOrThrow().contains(6u) }
                .size
        )


        val ctx2 = TriggProvider()
        val groupAdder2 = ctx2.group(5u) {
            add { SimpleObject(5u, Pos.ZERO) }
            group(6u) {
                add { SimpleObject(5u, Pos.ZERO) }
                add { TimewarpTrigger(Pos.ZERO, 1.65f) }

                spawn {
                    add { SimpleObject(5u, Pos.ZERO) }
                    add { TimewarpTrigger(Pos.ZERO, 1.65f) }
                }
            }
        }

        Assertions.assertFalse {
            groupAdder2.collectedObjects
                .all { it.groups.getOrThrow().contains(5u) && it.groups.getOrThrow().contains(6u) }
        }
        Assertions.assertTrue {
            groupAdder2.collectedObjects
                .all { it.groups.getOrThrow().contains(5u) && it.groups.getOrThrow().contains(ctx.init.commonGroup.value) }
        }

        Assertions.assertEquals(
            2 + 1, // 2 added objects + 1 implicit spawn trigger
            groupAdder2.collectedObjects
                .filter { it.groups.getOrThrow().contains(6u) }
                .size
        )

        Assertions.assertEquals(
            2,
            groupAdder2.collectedObjects
                // 1u would be the acquired group for the spawn branch
                .filter { it.groups.getOrThrow().contains(1u) }
                .size
        )
    }

    @Test
    @DisplayName("ObjectManager.groups test")
    fun groupsTest() {
        val ctx = TriggProvider()
        val groupAdder = ctx.groups(5u, 9u) {
            add { SimpleObject(5u, Pos.ZERO) }
            add { TimewarpTrigger(Pos.ZERO, 1.65f) }
        }

        Assertions.assertTrue {
            groupAdder.collectedObjects
                .all { it.groups.getOrThrow().contains(5u) && it.groups.getOrThrow().contains(9u) && it.groups.getOrThrow().contains(ctx.init.commonGroup.value) }
        }
        Assertions.assertEquals(2, groupAdder.collectedObjects.size)


        val ctx2 = TriggProvider()
        val groupAdder2 = ctx2.groups(5u, 9u) {
            add { SimpleObject(5u, Pos.ZERO) }
            add { TimewarpTrigger(Pos.ZERO, 1.65f) }

            spawn {
                add { SimpleObject(5u, Pos.ZERO) }
                add { TimewarpTrigger(Pos.ZERO, 1.65f) }
            }
        }

        Assertions.assertFalse {
            groupAdder2.collectedObjects
                .all { it.groups.getOrThrow().contains(5u) && it.groups.getOrThrow().contains(9u) && it.groups.getOrThrow().contains(ctx.init.commonGroup.value) }
        }

        Assertions.assertEquals(
            2 + 1, // 2 added objects + 1 implicit spawn trigger
            groupAdder2.collectedObjects
                .filter { it.groups.getOrThrow().contains(5u) && it.groups.getOrThrow().contains(9u) }
                .size
        )
        Assertions.assertEquals(
            2,
            groupAdder2.collectedObjects
                // 1u would be the acquired group for the spawn branch
                .filter { it.groups.getOrThrow().contains(1u) }
                .size
        )
    }

    @Test
    @DisplayName("ObjectManager.groups test (nested)")
    fun nestedGroupsTest() {
        val ctx = TriggProvider()
        val groupAdder = ctx.groups(5u, 9u) {
            add { SimpleObject(2u, Pos.ZERO) }
            group(6u) {
                add { SimpleObject(5u, Pos.ZERO) }
                add { TimewarpTrigger(Pos.ZERO, 1.65f) }
            }
        }

        Assertions.assertFalse {
            groupAdder.collectedObjects
                .all { it.groups.getOrThrow().contains(5u) && it.groups.getOrThrow().contains(9u) && it.groups.getOrThrow().contains(6u) }
        }
        Assertions.assertTrue {
            groupAdder.collectedObjects
                .all { it.groups.getOrThrow().contains(5u) && it.groups.getOrThrow().contains(9u) && it.groups.getOrThrow().contains(ctx.init.commonGroup.value) }
        }

        Assertions.assertEquals(
            2,
            groupAdder.collectedObjects
                // 1u would be the acquired group for the spawn branch
                .filter { it.groups.getOrThrow().contains(6u) }
                .size
        )


        val ctx2 = TriggProvider()
        val groupAdder2 = ctx2.groups(5u, 9u)  {
            add { SimpleObject(5u, Pos.ZERO) }
            group(6u) {
                add { SimpleObject(5u, Pos.ZERO) }
                add { TimewarpTrigger(Pos.ZERO, 1.65f) }

                spawn {
                    add { SimpleObject(5u, Pos.ZERO) }
                    add { TimewarpTrigger(Pos.ZERO, 1.65f) }
                }
            }
        }

        Assertions.assertFalse {
            groupAdder2.collectedObjects
                .all { it.groups.getOrThrow().contains(5u) && it.groups.getOrThrow().contains(9u) && it.groups.getOrThrow().contains(6u) }
        }
        Assertions.assertTrue {
            groupAdder2.collectedObjects
                .all { it.groups.getOrThrow().contains(5u) && it.groups.getOrThrow().contains(9u) && it.groups.getOrThrow().contains(ctx.init.commonGroup.value) }
        }

        Assertions.assertEquals(
            2 + 1, // 2 added objects + 1 implicit spawn trigger
            groupAdder2.collectedObjects
                .filter { it.groups.getOrThrow().contains(6u) }
                .size
        )

        Assertions.assertEquals(
            2,
            groupAdder2.collectedObjects
                // 1u would be the acquired group for the spawn branch
                .filter { it.groups.getOrThrow().contains(1u) }
                .size
        )
    }


    // "onlyGroup" uses "onlyGroups" so we don't add
    // tests for "onlyGroup"

    // In a "non nested" scenario, "onlyGroups"
    // does the same thing as "groups"
    @Test
    @DisplayName("ObjectManager.onlyGroups test")
    fun onlyGroupsTest() {
        val ctx = TriggProvider()

        // "onlyGroups" requires this so yeah !!!
        TriggProvider.INSTANCE = ctx

        val groupAdder = onlyGroups(5u, 9u) {
            add { SimpleObject(5u, Pos.ZERO) }
            add { TimewarpTrigger(Pos.ZERO, 1.65f) }
        }

        Assertions.assertTrue {
            groupAdder.collectedObjects
                .all { it.groups.getOrThrow().contains(5u) && it.groups.getOrThrow().contains(9u) && it.groups.getOrThrow().contains(ctx.init.commonGroup.value) }
        }
        Assertions.assertEquals(2, groupAdder.collectedObjects.size)


        TriggProvider.INSTANCE = null
        val ctx2 = TriggProvider()
        TriggProvider.INSTANCE = ctx2

        val groupAdder2 = onlyGroups(5u, 9u) {
            add { SimpleObject(5u, Pos.ZERO) }
            add { TimewarpTrigger(Pos.ZERO, 1.65f) }

            spawn {
                add { SimpleObject(5u, Pos.ZERO) }
                add { TimewarpTrigger(Pos.ZERO, 1.65f) }
            }
        }

        Assertions.assertFalse {
            groupAdder2.collectedObjects
                .all { it.groups.getOrThrow().contains(5u) && it.groups.getOrThrow().contains(9u) && it.groups.getOrThrow().contains(ctx.init.commonGroup.value) }
        }

        Assertions.assertEquals(
            2 + 1, // 2 added objects + 1 implicit spawn trigger
            groupAdder2.collectedObjects
                .filter { it.groups.getOrThrow().contains(5u) && it.groups.getOrThrow().contains(9u) }
                .size
        )
        Assertions.assertEquals(
            2,
            groupAdder2.collectedObjects
                // 1u would be the acquired group for the spawn branch
                .filter { it.groups.getOrThrow().contains(1u) }
                .size
        )
    }

    @Test
    @DisplayName("ObjectManager.onlyGroups test (nested)")
    fun nestedOnlyGroupsTest() {
        val ctx = TriggProvider()

        // "onlyGroups" requires this so yeah !!!
        TriggProvider.INSTANCE = ctx

        val nestedGroupAdder: GroupAdder
        val groupAdder = ctx.group(5u) {
            add { SimpleObject(1u, Pos.ZERO) }

            nestedGroupAdder = onlyGroups(8u, 3u) {
                add { SimpleObject(5u, Pos.ZERO) }
                add { TimewarpTrigger(Pos.ZERO, 1.65f) }
            }
        }

        Assertions.assertEquals(1, groupAdder.collectedObjects.size)
        Assertions.assertEquals(2, nestedGroupAdder.collectedObjects.size)

        Assertions.assertTrue {
            nestedGroupAdder.collectedObjects
                .all { it.groups.getOrThrow().contains(8u) && it.groups.getOrThrow().contains(3u) && it.groups.getOrThrow().contains(ctx.init.commonGroup.value) }
        }


        TriggProvider.INSTANCE = null
        val ctx2 = TriggProvider()
        TriggProvider.INSTANCE = ctx2

        lateinit var groupAdder2: GroupAdder
        lateinit var nestedGroupAdder2: GroupAdder

        ctx.spawn {
            groupAdder2 = this as GroupAdder

            add { SimpleObject(1u, Pos.ZERO) }

            nestedGroupAdder2 = onlyGroups(8u, 3u) {
                add { SimpleObject(5u, Pos.ZERO) }
                add { TimewarpTrigger(Pos.ZERO, 1.65f) }
            }
        }

        Assertions.assertEquals(1, groupAdder2.collectedObjects.size)
        Assertions.assertEquals(2, nestedGroupAdder2.collectedObjects.size)

        Assertions.assertTrue {
            groupAdder2.collectedObjects
                .all { it.groups.getOrThrow().contains(1u) && it.groups.getOrThrow().contains(ctx.init.commonGroup.value) }
        }

        Assertions.assertTrue {
            nestedGroupAdder2.collectedObjects
                .all { it.groups.getOrThrow().contains(8u) && it.groups.getOrThrow().contains(3u) && it.groups.getOrThrow().contains(ctx.init.commonGroup.value) }
        }
    }

    @Test
    fun acquireRegisterTest() {
        val ctx = TriggProvider()
        ctx.init {
            labelSpace {
                baseOffset = Random.nextUInt(1u..50u)
            }
        }

        for (registerID in ctx.init.labelSpace.registerRange)
            Assertions.assertEquals(registerID, ctx.acquireRegister())

        Assertions.assertEquals(ctx.init.labelSpace.registers.toInt(), ctx.acquiredRegisters.size)
        Assertions.assertThrows(IndexOutOfBoundsException::class.java) {
            ctx.acquireRegister()
        }
    }

    @Test
    fun releaseRegisterTest() {
        val ctx = TriggProvider()
        ctx.init {
            labelSpace {
                baseOffset = Random.nextUInt(1u..50u)
            }
        }

        for (registerID in ctx.init.labelSpace.registerRange)
            Assertions.assertEquals(registerID, ctx.acquireRegister())

        Assertions.assertEquals(ctx.init.labelSpace.registers.toInt(), ctx.acquiredRegisters.size)
        Assertions.assertThrows(IndexOutOfBoundsException::class.java) {
            ctx.acquireRegister()
        }


        val originalAcquiredRegistersSize = ctx.acquiredRegisters.size

        val registersToRelease = ctx.acquiredRegisters.shuffled()
        registersToRelease.forEachIndexed { index, register ->
            ctx.releaseRegister(register)
            Assertions.assertEquals(originalAcquiredRegistersSize - (index + 1), ctx.acquiredRegisters.size)
        }

        Assertions.assertThrows(IllegalArgumentException::class.java) {
            ctx.releaseRegister(Random.nextUInt())
        }
    }
}