package generator

import editor.objects.GenericGdObject
import editor.objects.SimpleObject
import editor.objects.triggers.misc.SpawnTrigger
import editor.objects.triggers.misc.TimewarpTrigger
import editor.objects.triggers.objects.ColorTrigger
import editor.objects.triggers.objects.SpecialColors
import editor.rawstring.property.getOrThrow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import provider.ObjectManager
import provider.TriggProvider
import provider.trigg
import java.awt.Color

@Suppress("unused", "EmptyMethod")
internal class TriggFunctionTests {
    @TriggFunction fun invalidTriggFunction() {}
    @TriggFunction fun invalidTriggFunction1(a: Int, b: Int) {}
    @TriggFunction fun invalidTriggFunction2(a: ObjectManager<SimpleObject>, b: Int) {}
    @TriggFunction fun invalidTriggFunction3(b: Int, a: ObjectManager<SimpleObject>) {}
    @TriggFunction fun invalidTriggFunction4(a: ObjectManager<SpawnTrigger>) {}
    @TriggFunction fun invalidTriggFunction5(a: ObjectManager<GenericGdObject>) {}
    fun invalidTriggFunction6(ctx: ObjectManager<SimpleObject>) {}

    @TriggFunction
    fun validTriggFunction(ctx: ObjectManager<SimpleObject>) {
        ctx.apply {
            add { TimewarpTrigger(getNextPos()) }
            add { ColorTrigger(getNextPos(), Color.GREEN, SpecialColors.BACKGROUND.colorID) }
        }
    }

    fun assertIsValid(triggFunction: TriggFunctionType) {
        Assertions.assertDoesNotThrow {
            TriggFunction.validate(triggFunction)
        }
    }

    fun assertIsNotValid(triggFunction: TriggFunctionType) {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            TriggFunction.validate(triggFunction)
        }
    }

    @AfterEach
    fun afterEach() {
        TriggProvider.INSTANCE = null
    }

    @Test
    fun triggFunctionValidatorTest() {
        assertIsValid(::validTriggFunction)

        assertIsNotValid(::invalidTriggFunction)
        assertIsNotValid(::invalidTriggFunction1)
        assertIsNotValid(::invalidTriggFunction2)
        assertIsNotValid(::invalidTriggFunction3)
        assertIsNotValid(::invalidTriggFunction4)
        assertIsNotValid(::invalidTriggFunction5)
        assertIsNotValid(::invalidTriggFunction6)
    }


    @Test
    fun getIdTest() {
        val ctx = TriggProvider()
        TriggProvider.INSTANCE = ctx

        Assertions.assertDoesNotThrow { ctx.getFunctionID(::validTriggFunction) }

        ctx.apply {
            defineFunction(::validTriggFunction)
        }

        Assertions.assertDoesNotThrow { ctx.getFunctionID(::validTriggFunction) }

        Assertions.assertEquals(ctx.triggFunctions[::validTriggFunction]!!.group.value, ctx.getFunctionID(::validTriggFunction))
    }

    @Test
    fun functionDefineTest() {
        val ctx = TriggProvider()
        TriggProvider.INSTANCE = ctx

        ctx.apply {
            defineFunction(::validTriggFunction)
        }

        Assertions.assertEquals(1, ctx.triggFunctions.size)
        Assertions.assertTrue(ctx.triggFunctions.containsKey(::validTriggFunction))
        Assertions.assertTrue(ctx.triggFunctions[::validTriggFunction]!!.hasBeenDefined)

        Assertions.assertEquals(2, ctx.addedObjects.size)
        Assertions.assertEquals(0, ctx.addedObjects.filterIsInstance<SpawnTrigger>().size)
    }

    @Test
    fun functionSpawnTest() {
        val ctx = TriggProvider()
        TriggProvider.INSTANCE = ctx

        val createdSpawnTrigger: SpawnTrigger
        ctx.apply {
            entrypoint(execute = false) {
                createdSpawnTrigger = spawnFunction(::validTriggFunction)
            }
        }

        Assertions.assertEquals(1, ctx.triggFunctions.size)
        Assertions.assertTrue(ctx.triggFunctions.containsKey(::validTriggFunction))
        Assertions.assertFalse(ctx.triggFunctions[::validTriggFunction]!!.hasBeenDefined)

        Assertions.assertEquals(1, ctx.addedObjects.size)
        Assertions.assertEquals(1, ctx.addedObjects.filterIsInstance<SpawnTrigger>().size)

        val spawnTrigger = ctx.addedObjects.filterIsInstance<SpawnTrigger>().first()
        Assertions.assertSame(createdSpawnTrigger, spawnTrigger)
        Assertions.assertTrue(spawnTrigger.spawnTriggered.getOrThrow())
        Assertions.assertTrue(spawnTrigger.multiTriggered.getOrThrow())
        Assertions.assertEquals(ctx.triggFunctions[::validTriggFunction]!!.group.value, spawnTrigger.targetGroup.getOrThrow())


        ctx.apply {
            defineFunction(::validTriggFunction)
        }

        Assertions.assertEquals(1, ctx.triggFunctions.size)
        Assertions.assertTrue(ctx.triggFunctions.containsKey(::validTriggFunction))
        Assertions.assertTrue(ctx.triggFunctions[::validTriggFunction]!!.hasBeenDefined)

        Assertions.assertEquals(1 + 2, ctx.addedObjects.size)
        Assertions.assertEquals(1, ctx.addedObjects.filterIsInstance<SpawnTrigger>().size)
    }

    @Test
    @DisplayName("TriggFunction without definition test")
    fun functionNoDefTest() {
        Assertions.assertThrows(IllegalStateException::class.java) {
            trigg {
                spawnFunction(::validTriggFunction)
            }
        }
    }
}