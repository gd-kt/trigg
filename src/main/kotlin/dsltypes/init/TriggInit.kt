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
    val basePos: Position
    val labelSpace: LabelSpace
    val groupSpace: GroupSpace
    /**
     * If the previous objects should be cleared.
     * This uses WSLiveEditor
     * @see commonGroup
     * @see editor.wsliveeditor.WSLiveEditor.addObjects
     */
    val clearPreviousObjects: Boolean
}

data class MutableTriggInit(
    val triggProvider: TriggProvider,
    override var commonGroup: Group = TriggerProperties.MAX_GROUPS.group,
    override var basePos: Position = Pos.ZERO,
    override var labelSpace: LabelSpace = LabelSpace.DEFAULT,
    override var groupSpace: GroupSpace = GroupSpace.DEFAULT,
    override var clearPreviousObjects: Boolean = false
) : TriggInit {
    fun toImmutable(): ImmutableTriggInit =
        ImmutableTriggInit(this.commonGroup, this.basePos, this.labelSpace, this.groupSpace, this.clearPreviousObjects)
}

data class ImmutableTriggInit(
    override val commonGroup: Group = TriggerProperties.MAX_GROUPS.group,
    override val basePos: Position = Pos.ZERO,
    override val labelSpace: LabelSpace = LabelSpace.DEFAULT,
    override val groupSpace: GroupSpace = GroupSpace.DEFAULT,
    override val clearPreviousObjects: Boolean = false
) : TriggInit
