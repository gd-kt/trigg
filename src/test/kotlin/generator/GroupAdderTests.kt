package generator

import dsltypes.init.TriggInit
import editor.objects.SimpleObject
import editor.objects.data.Pos
import editor.objects.triggers.misc.TimewarpTrigger
import editor.rawstring.property.getOrThrow
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import provider.TriggProvider

private class GroupAdderTests {
    @Test
    @DisplayName("Without calling spawn()")
    fun noSpawnAdder() {
        val objAdder = GroupAdder(TriggProvider(), 5u)
        objAdder.add { SimpleObject(5u, Pos.ZERO) }
        objAdder.add { TimewarpTrigger(Pos.ZERO, 1.5f) }

        Assertions.assertTrue(objAdder.collectedObjects.all {
            it.groups.getOrThrow().contains(5u)
                    && it.groups.getOrThrow().contains(TriggInit.DEFAULT.baseGroup)
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

        Assertions.assertFalse(objAdder.collectedObjects.all {
            it.groups.getOrThrow().contains(5u)
                    && it.groups.getOrThrow().contains(TriggInit.DEFAULT.baseGroup)
                    && it.groups.size() == 1
        })

        Assertions.assertTrue(objAdder.spawnBranches.flatten().all {
            // 6u is the acquired group for the spawn trigger
            it.groups.getOrThrow().contains(6u)
                    && it.groups.getOrThrow().contains(TriggInit.DEFAULT.baseGroup)
                    && it.groups.size() == 2
        })
    }
}