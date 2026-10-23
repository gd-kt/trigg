package dsltypes.init.space

import Group
import Group.Companion.group
import dsltypes.init.MutableTriggInit
import utils.groupClamping

interface LabelSpace {
    companion object {
        val DEFAULT = ImmutableLabelSpace()
    }

    /**
     * The number of allowed labels for the memory of the script.
     * Owned by `trigg`.
     */
    val registers: Group

    /**
     * The number of allowed labels for the memory of the script.
     * Owned by the user.
     */
    val script: Group

    /**
     * The offset at which labels are instantiated.
     */
    val baseOffset: Group

    /**
     * The offset at which **registers start** (inclusive)
     */
    val registerOffset: Group
        get() = this.baseOffset + 1.group

    /**
     * The offset at which **script labels start** (inclusive)
     */
    val scriptOffset: Group
        get() = this.registerOffset + 1.group + this.registers

    /**
     * The range in which registers are contained
     */
    val registerRange: UIntRange
        get() = ((this.baseOffset.value + 1u)..(this.baseOffset.value + 1u + this.registers.value)).groupClamping()

    /**
     * The range in which script labels are contained
     */
    val scriptRange: UIntRange
        get() = ((this.scriptOffset.value)..(this.registerRange.last + 1u + this.script.value)).groupClamping()
}

data class MutableLabelSpace(override var registers: Group = 10.group, override var script: Group = 100.group, override var baseOffset: Group = 0.group): LabelSpace {
    init {
        require(this.registers >= 1.group)
        require(this.script >= 1.group)
    }

    fun toImmutable(): ImmutableLabelSpace =
        ImmutableLabelSpace(this.registers, this.script, this.baseOffset)
}

data class ImmutableLabelSpace(override val registers: Group = 10.group, override val script: Group = 100.group, override val baseOffset: Group = 0.group): LabelSpace {
    init {
        require(this.registers >= 1.group)
        require(this.script >= 1.group)
    }
}

fun MutableTriggInit.labelSpace(labelSpaceSetter: MutableLabelSpace.() -> Unit) {
    val labelSpace = MutableLabelSpace()
    labelSpaceSetter(labelSpace)

    require(this.labelSpace === LabelSpace.DEFAULT) { "Cannot set twice the label space of a trigg dsl !" }
    this.labelSpace = labelSpace.toImmutable()
}
