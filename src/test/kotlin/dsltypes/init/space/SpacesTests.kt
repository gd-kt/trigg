package dsltypes.init.space

import Group.Companion.group
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import utils.overlaps

private class LabelSpaceTests {
    @Test
    fun rangeTests() {
        val labelSpace = MutableLabelSpace()
        Assertions.assertEquals(1u..(labelSpace.registers.value + 1u), labelSpace.registerRange)

        labelSpace.baseOffset = 1.group
        Assertions.assertEquals(2u..(labelSpace.registers.value + 2u), labelSpace.registerRange)

        labelSpace.baseOffset = 8.group
        Assertions.assertEquals(labelSpace.scriptRange.first..(labelSpace.registerRange.last + 1u + labelSpace.script.value), labelSpace.scriptRange)
        Assertions.assertFalse(labelSpace.registerRange.overlaps(labelSpace.scriptRange))

        Assertions.assertEquals(labelSpace.scriptRange.first, labelSpace.scriptOffset.value)
    }
}

private class GroupSpaceTests {
    @Test
    fun rangeTests() {
        val groupSpace = MutableGroupSpace()
        Assertions.assertEquals(9998.group, groupSpace.entrypoint)

        Assertions.assertEquals(1u..(groupSpace.allocatedGroups.value + 1u), groupSpace.groupRange)

        groupSpace.allocatedGroups = 27.group
        groupSpace.baseOffset = 3.group
        Assertions.assertEquals(4u..(27u + 4u), groupSpace.groupRange)

        groupSpace.allocatedGroups = 5.group
        groupSpace.baseOffset = 2.group
        Assertions.assertEquals(3u..(5u + 3u), groupSpace.groupRange)

        groupSpace.groupRange = 7u..11u
        Assertions.assertEquals(7u..11u, groupSpace.groupRange)
        Assertions.assertEquals(6u, groupSpace.baseOffset.value)
        Assertions.assertEquals(11u - 7u, groupSpace.allocatedGroups.value)
    }
}
