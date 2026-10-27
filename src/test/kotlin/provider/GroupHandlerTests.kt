package provider

import Group.Companion.group
import dsltypes.init.space.groupSpace
import dsltypes.init.space.labelSpace
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import oldEntrypointInit

private class GroupHandlerTests {
    @Test
    fun acquireGroupTest() {
        val ctx = TriggProvider()
        Assertions.assertEquals(ctx.init.groupSpace.baseOffset.value + 1u, ctx.acquireGroup())
        Assertions.assertEquals(ctx.init.groupSpace.baseOffset.value + 2u, ctx.acquireGroup())

        val ctx2 = TriggProvider()
        ctx2.oldEntrypointInit {
            groupSpace {
                baseOffset = 5.group
            }
        }

        Assertions.assertEquals(5.group, ctx2.init.groupSpace.baseOffset)
        Assertions.assertEquals(6u, ctx2.acquireGroup())
        Assertions.assertEquals(7u, ctx2.acquireGroup())
    }

    @Test
    fun acquireLabelTest() {
        val ctx = TriggProvider()
        Assertions.assertEquals(ctx.init.labelSpace.scriptRange.first, ctx.acquireLabel())
        Assertions.assertEquals(ctx.init.labelSpace.scriptRange.first + 1u, ctx.acquireLabel())

        val ctx2 = TriggProvider()
        ctx2.init {
            labelSpace {
                baseOffset = 2u
                registers = 2u
                script = 76u
            }
        }

        val scriptLabelsOffset = (2u + 1u) + (2u + 1u)
        Assertions.assertEquals(scriptLabelsOffset, ctx2.init.labelSpace.scriptOffset)
        Assertions.assertEquals(scriptLabelsOffset, ctx2.acquireLabel())
        Assertions.assertEquals(scriptLabelsOffset + 1u, ctx2.acquireLabel())
    }
}