package dsltypes.values

import OldEntrypointTriggProvider
import editor.objects.triggers.counters.PersistentTrigger
import editor.rawstring.property.getOrThrow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import provider.TriggProvider

private class PersistencyTests {
    @AfterEach
    fun afterEach() {
        TriggProvider.INSTANCE = null
    }

    @Test
    @DisplayName("With ON_LEVEL_START flag")
    fun levelStartTest() {
        val ctx = TriggProvider()
        TriggProvider.INSTANCE = ctx

        val value = item(8u)
        val persistentValue = ctx.persistent(PersistentType.ON_LEVEL_START) { value }

        Assertions.assertSame(value, persistentValue)
        Assertions.assertEquals(1, ctx.addedObjects.size)

        val persistentTrigger = ctx.addedObjects
            .filterIsInstance<PersistentTrigger>()
            .first()

        Assertions.assertTrue(persistentTrigger.setPersistent.getOrThrow())
        Assertions.assertFalse(persistentTrigger.isTimer.getOrThrow())
        Assertions.assertFalse(persistentTrigger.resetSelectedItems.getOrThrow())
        Assertions.assertFalse(persistentTrigger.targetAllPersistentItems.getOrThrow())
        Assertions.assertEquals(value.itemID, persistentTrigger.itemID.value)


        TriggProvider.INSTANCE = null
        val ctx2 = OldEntrypointTriggProvider()
        TriggProvider.INSTANCE = ctx2

        ctx2.entrypoint(execute = false) {
            spawn {
                persistent(PersistentType.ON_LEVEL_START) { value }
            }
        }

        val persistentTrigger2 = ctx2.addedObjects
            .filterIsInstance<PersistentTrigger>()
            .first()

        Assertions.assertEquals(
            1,
            ctx2.addedObjects
                .filterIsInstance<PersistentTrigger>()
                .size
        )

        // 2u would be the acquired group for the spawn trigger
        // Since flag = PersistentType.ON_LEVEL_START, the persistent
        // trigger doesn't get added to the entrypoint/spawn trigger
        Assertions.assertFalse(persistentTrigger2.groups.contains(2u))
        Assertions.assertFalse(persistentTrigger2.groups.contains(ctx2.init.groupSpace.getEntrypoint(ctx2).value))

        Assertions.assertEquals(persistentTrigger.groups, persistentTrigger2.groups)
    }

    @Test
    @DisplayName("With ON_TRIGGER flag")
    fun onTriggerTest() {
        val ctx = TriggProvider()

        val value = item(8u)
        val persistentValue = ctx.persistent(PersistentType.ON_TRIGGER) { value }

        Assertions.assertSame(value, persistentValue)
        Assertions.assertEquals(1, ctx.addedObjects.size)

        val persistentTrigger = ctx.addedObjects
            .filterIsInstance<PersistentTrigger>()
            .first()

        Assertions.assertTrue(persistentTrigger.setPersistent.getOrThrow())
        Assertions.assertFalse(persistentTrigger.isTimer.getOrThrow())
        Assertions.assertFalse(persistentTrigger.resetSelectedItems.getOrThrow())
        Assertions.assertFalse(persistentTrigger.targetAllPersistentItems.getOrThrow())
        Assertions.assertEquals(value.itemID, persistentTrigger.itemID.value)


        val ctx2 = OldEntrypointTriggProvider()

        ctx2.entrypoint(execute = false) {
            spawn {
                persistent(PersistentType.ON_TRIGGER) { value }
            }
        }

        val persistentTrigger2 = ctx2.addedObjects
            .filterIsInstance<PersistentTrigger>()
            .first()

        Assertions.assertEquals(
            1,
            ctx2.addedObjects
                .filterIsInstance<PersistentTrigger>()
                .size
        )

        // 1u would be the acquired group for the spawn trigger
        // Since flag = PersistentType.ON_LEVEL_START, the persistent
        // trigger doesn't get added to the entrypoint/spawn trigger
        Assertions.assertTrue(persistentTrigger2.groups.contains(1u))
        Assertions.assertFalse(persistentTrigger2.groups.contains(ctx2.init.groupSpace.getEntrypoint(ctx2).value))

        Assertions.assertNotEquals(persistentTrigger.groups, persistentTrigger2.groups)
    }
}