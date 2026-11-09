package dsltypes.init

import Group
import Group.Companion.group
import TriggDsl
import dsltypes.init.space.GroupSpace
import dsltypes.init.space.LabelSpace
import editor.objects.data.Pos
import editor.objects.data.Position
import editor.objects.propertycontainers.TriggerProperties
import provider.TriggProvider

@TriggDsl
interface TriggInit {
    companion object {
        val DEFAULT = ImmutableTriggInit()
    }

    /**
     * The main group added to all objects created by `trigg`
     * @see clearPreviousObjects
     */
    val commonGroup: Group

    /**
     * The first pos of the placed trigg triggers
     */
    val basePos: Position

    /**
     * If the placed triggers should be offseted to show the tree
     * of the trigg script
     */
    val beautifyTriggers: Boolean

    val labelSpace: LabelSpace

    val groupSpace: GroupSpace

    /**
     * If the previous objects should be cleared.
     * This uses WSLiveEditor
     * @see commonGroup
     * @see editor.wsliveeditor.WSLiveEditor.removeObjects
     */
    val clearPreviousObjects: Boolean
}

data class MutableTriggInit(
    val triggProvider: TriggProvider,
    override var commonGroup: Group = TriggerProperties.MAX_GROUPS.group,
    override var basePos: Position = Pos.gridCentered(5f, 3f),
    override val beautifyTriggers: Boolean = true,
    override var labelSpace: LabelSpace = LabelSpace.DEFAULT,
    override var groupSpace: GroupSpace = GroupSpace.DEFAULT,
    override var clearPreviousObjects: Boolean = false
) : TriggInit {
    fun toImmutable(): ImmutableTriggInit =
        ImmutableTriggInit(
            this.commonGroup,
            this.basePos,
            this.beautifyTriggers,
            this.labelSpace,
            this.groupSpace,
            this.clearPreviousObjects
        )
}

data class ImmutableTriggInit(
    override val commonGroup: Group = TriggerProperties.MAX_GROUPS.group,
    override val basePos: Position = Pos.gridCentered(5f, 3f),
    override val beautifyTriggers: Boolean = true,
    override val labelSpace: LabelSpace = LabelSpace.DEFAULT,
    override val groupSpace: GroupSpace = GroupSpace.DEFAULT,
    override val clearPreviousObjects: Boolean = false
) : TriggInit
