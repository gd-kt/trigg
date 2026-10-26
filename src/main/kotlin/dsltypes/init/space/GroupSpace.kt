package dsltypes.init.space

import Group
import Group.Companion.group
import dsltypes.init.MutableTriggInit
import editor.objects.propertycontainers.TriggerProperties
import provider.GroupHandler
import utils.groupClamping

interface GroupSpace {
    companion object {
        val DEFAULT = MutableGroupSpace().toImmutable()
    }

    /**
     * The group used as an entrypoint.
     * If `null` then a new group will be [acquired][provider.GroupHandler.acquireGroup] as
     * the entrypoint.
     *
     * The acquired entrypoint group can be fetched
     * using [getEntrypoint]. If the group was manually set instead
     * and is non-null, [getEntrypoint] will return it.
     */
    val entrypoint: Group?

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

    /**
     * Gets the entrypoint's group
     * @param ctx the context used to acquire a group, if needed
     * @return the entrypoint's group
     * @see entrypoint
     */
    fun getEntrypoint(ctx: GroupHandler): Group
}

data class MutableGroupSpace(
    override var entrypoint: Group? = null,
    override var baseOffset: Group = 0.group,
    override var allocatedGroups: Group = 100.group
): GroupSpace {
    init {
        if (this.entrypoint != null)
            require(this.entrypoint!! >= 1.group)
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

    fun getOrSetEntrypoint(ctx: GroupHandler): Group {
        if (this.entrypoint == null)
            this.entrypoint = ctx.acquireGroup().group

        return this.entrypoint!!
    }

    override fun getEntrypoint(ctx: GroupHandler) =
        this.getOrSetEntrypoint(ctx)

    fun toImmutable(): ImmutableGroupSpace =
        ImmutableGroupSpace(
            this.entrypoint,
            this.baseOffset,
            this.allocatedGroups
        )
}

data class ImmutableGroupSpace(
    override val entrypoint: Group?,
    override val baseOffset: Group,
    override val allocatedGroups: Group,
    private var actualEntrypoint: Group? = entrypoint
): GroupSpace {
    init {
        if (this.entrypoint != null)
            require(this.entrypoint >= 1.group)
        require(this.allocatedGroups >= 1.group)
    }

    private fun getOrSetEntrypoint(ctx: GroupHandler): Group {
        if (this.entrypoint != null)
            return this.entrypoint

        if (this.actualEntrypoint == null)
            this.actualEntrypoint = ctx.acquireGroup().group

        return this.actualEntrypoint!!
    }

    override fun getEntrypoint(ctx: GroupHandler): Group =
        this.getOrSetEntrypoint(ctx)
}

fun MutableTriggInit.groupSpace(groupSpaceSetter: MutableGroupSpace.() -> Unit) {
    require(this.groupSpace === GroupSpace.DEFAULT) { "Cannot set twice the group space of a trigg dsl !" }

    val groupSpace = MutableGroupSpace()
    groupSpaceSetter(groupSpace)

    this.groupSpace = groupSpace.toImmutable()
}
