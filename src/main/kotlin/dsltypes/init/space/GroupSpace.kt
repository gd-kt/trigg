package dsltypes.init.space

import Group
import Group.Companion.group
import dsltypes.init.MutableTriggInit
import editor.objects.propertycontainers.TriggerProperties
import utils.groupClamping

interface GroupSpace {
    companion object {
        val DEFAULT = ImmutableGroupSpace()
    }

    /**
     * The group used as an entrypoint
     */
    val entrypoint: Group // TODO: Nullable + null as default. If null will acquire a new group as entrypoint

    /**
     * The offset for all groups
     */
    val baseOffset: Group

    /**
     * The number of allocated groups
     */
    val allocatedGroups: Group

    /**
     * The range of groups this [GroupSpace]
     * has allocated. Ranges from `[baseOffset + 1, allocatedGroups + baseOffset + 1]`
     */
    val groupRange: UIntRange
        get() = ((this.baseOffset.value + 1u)..(this.allocatedGroups.value + this.baseOffset.value + 1u)).groupClamping()
}

data class MutableGroupSpace(
    override var entrypoint: Group = (TriggerProperties.MAX_GROUPS - 1u).group,
    override var baseOffset: Group = 0.group,
    override var allocatedGroups: Group = 100.group
): GroupSpace {
    init {
        require(this.entrypoint >= 1.group)
        require(this.allocatedGroups >= 1.group)
    }

    override var groupRange: UIntRange
        get() = super.groupRange
        set(value) {
            require(value.first >= 1u)
            require(value.last <= TriggerProperties.MAX_GROUPS)

            this.baseOffset = value.first.group - 1u
            this.allocatedGroups = (value.last - value.first).group
        }

    fun toImmutable(): ImmutableGroupSpace =
        ImmutableGroupSpace(this.entrypoint, this.baseOffset)
}

data class ImmutableGroupSpace(
    override val entrypoint: Group = (TriggerProperties.MAX_GROUPS - 1u).group,
    override val baseOffset: Group = 0.group,
    override val allocatedGroups: Group = 100.group
): GroupSpace {
    init {
        require(this.entrypoint >= 1.group)
        require(this.allocatedGroups >= 1.group)
    }
}

fun MutableTriggInit.groupSpace(groupSpaceSetter: MutableGroupSpace.() -> Unit) {
    val groupSpace = MutableGroupSpace()
    groupSpaceSetter(groupSpace)

    require(this.groupSpace === GroupSpace.DEFAULT) { "Cannot set twice the group space of a trigg dsl !" }
    this.groupSpace = groupSpace.toImmutable()
}
