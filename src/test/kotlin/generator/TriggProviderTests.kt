package generator

import editor.objects.SimpleObject
import editor.objects.data.Pos
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import provider.ObjectManager
import provider.TriggProvider
import provider.trigg

internal class TriggProviderTests {
    @Suppress("EmptyMethod")
    @TriggFunction
    fun testFunction(ctx: ObjectManager<SimpleObject>) {}

    @AfterEach
    fun afterEach() {
        TriggProvider.INSTANCE = null
    }

    @Test
    fun lockedInitTest() {
        val tempCtx = TriggProvider()
        Assertions.assertDoesNotThrow {
            tempCtx.init {  }
        }

        val ctx = TriggProvider()
        Assertions.assertFalse(ctx.isInitLocked)

        ctx.isInitLocked = false
        ctx.add { SimpleObject(1u, Pos.ZERO) }
        Assertions.assertTrue(ctx.isInitLocked)

        ctx.isInitLocked = false
        Assertions.assertNotEquals(ctx.getNextPos(), ctx.getNextPos())
        Assertions.assertTrue(ctx.isInitLocked)

        ctx.isInitLocked = false
        ctx.acquireGroup()
        Assertions.assertTrue(ctx.isInitLocked)

        ctx.isInitLocked = false
        ctx.acquireLabel()
        Assertions.assertTrue(ctx.isInitLocked)

        Assertions.assertThrows(IllegalStateException::class.java) {
            ctx.init {  }
        }
    }

    @Test
    fun isFinishedTest() {
        val toTest: Array<(TriggProvider) -> Unit> = arrayOf(
            { it.init { } },
            { it.spawn { } },
            { it.add { SimpleObject(1u, Pos.ZERO) } },
            { it.entrypoint { } },
            { it.group(7u) { } },
            { it.defineFunction(::testFunction) },
            { it.spawnFunction(::testFunction) },

            { it.acquireLabel() },
            { it.acquireGroup() }
        )

        val ctx1 = TriggProvider()
        Assertions.assertFalse(ctx1.isFinished)
        Assertions.assertDoesNotThrow {
            toTest.forEach { lambda -> lambda(ctx1) }
        }

        val ctx2 = TriggProvider()
        ctx2.isFinished = true

        toTest.forEachIndexed { index, lambda ->
            println("[TriggProviderTests.isFinishedTest] Testing index $index")
            Assertions.assertThrows(IllegalStateException::class.java) {
                lambda(ctx2)
            }
        }

        // ctx.getNextPos() is frozen when
        // isFinished == true
        Assertions.assertEquals(ctx2.getNextPos(), ctx2.getNextPos())
    }

    @Test
    fun doubleTriggInstantiationTest() {
        Assertions.assertDoesNotThrow {
            trigg {

            }
        }

        Assertions.assertThrows(IllegalStateException::class.java) {
            trigg {
                trigg {  }
            }
        }
    }

    @Test
    fun doubleInitTest() {
        Assertions.assertDoesNotThrow {
            trigg {
                init {  }
            }
        }

        Assertions.assertThrows(IllegalStateException::class.java) {
            trigg {
                init {  }
                init {  }
            }
        }
    }

    @Test
    fun doubleEntrypointTest() {
        Assertions.assertDoesNotThrow {
            trigg {
                entrypoint(execute = true) {  }
            }
        }

        Assertions.assertDoesNotThrow {
            trigg {
                entrypoint(execute = false) {  }
            }
        }


        Assertions.assertThrows(IllegalStateException::class.java) {
            trigg {
                entrypoint(execute = false) {  }
                entrypoint(execute = false) {  }
            }
        }

        Assertions.assertThrows(IllegalStateException::class.java) {
            trigg {
                entrypoint(execute = true) {  }
                entrypoint(execute = false) {  }
            }
        }

        Assertions.assertThrows(IllegalStateException::class.java) {
            trigg {
                entrypoint(execute = false) {  }
                entrypoint(execute = true) {  }
            }
        }

        Assertions.assertThrows(IllegalStateException::class.java) {
            trigg {
                entrypoint(execute = true) {  }
                entrypoint(execute = true) {  }
            }
        }
    }
}