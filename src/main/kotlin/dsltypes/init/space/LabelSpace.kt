package dsltypes.init.space

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
    val registers: UInt

    /**
     * The number of allowed labels for the memory of the script.
     * Owned by the user.
     */
    val script: UInt

    /**
     * The offset at which labels are instantiated.
     */
    val baseOffset: UInt

    /**
     * The offset at which **registers start** (inclusive)
     */
    val registerOffset: UInt
        get() = this.baseOffset + 1u

    /**
     * The offset at which **script labels start** (inclusive)
     */
    val scriptOffset: UInt
        get() = this.registerOffset + 1u + this.registers

    /**
     * The range in which registers are contained
     */
    val registerRange: UIntRange
        get() = ((this.baseOffset + 1u)..(this.baseOffset + 1u + this.registers)).groupClamping()

    /**
     * The range in which script labels are contained
     */
    val scriptRange: UIntRange
        get() = ((this.scriptOffset)..(this.registerRange.last + 1u + this.script)).groupClamping()
}

data class MutableLabelSpace(override var registers: UInt = 10u, override var script: UInt = 100u, override var baseOffset: UInt = 0u): LabelSpace {
    init {
        require(this.registers >= 1u)
        require(this.script >= 1u)
    }

    fun toImmutable(): ImmutableLabelSpace =
        ImmutableLabelSpace(this.registers, this.script, this.baseOffset)
}

data class ImmutableLabelSpace(override val registers: UInt = 10u, override val script: UInt = 100u, override val baseOffset: UInt = 0u): LabelSpace {
    init {
        require(this.registers >= 1u)
        require(this.script >= 1u)
    }
}

fun MutableTriggInit.labelSpace(labelSpaceSetter: MutableLabelSpace.() -> Unit) {
    val labelSpace = MutableLabelSpace()
    labelSpaceSetter(labelSpace)

    require(this.labelSpace === LabelSpace.DEFAULT) { "Cannot set twice the label space of a trigg dsl !" }
    this.labelSpace = labelSpace.toImmutable()
}
