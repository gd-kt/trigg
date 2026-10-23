package dsltypes.init

import TriggDsl
import dsltypes.init.space.GroupSpace
import dsltypes.init.space.LabelSpace
import editor.objects.data.Pos
import editor.objects.data.Position
import editor.objects.propertycontainers.TriggerProperties

@TriggDsl
interface TriggInit {
    companion object {
        val DEFAULT = ImmutableTriggInit()
    }

    /**
     * The main group added to all objects created by `trigg`
     */
    val baseGroup: UInt
    val basePos: Position
    val labelSpace: LabelSpace
    val groupSpace: GroupSpace
}

data class MutableTriggInit(
    override var baseGroup: UInt = TriggerProperties.MAX_GROUPS,
    override var basePos: Position = Pos.ZERO,
    override var labelSpace: LabelSpace = LabelSpace.DEFAULT,
    override var groupSpace: GroupSpace = GroupSpace.DEFAULT
) : TriggInit {
    fun toImmutable(): ImmutableTriggInit =
        ImmutableTriggInit(this.baseGroup, this.basePos, this.labelSpace, this.groupSpace)
}

data class ImmutableTriggInit(
    override val baseGroup: UInt = TriggerProperties.MAX_GROUPS,
    override val basePos: Position = Pos.ZERO,
    override val labelSpace: LabelSpace = LabelSpace.DEFAULT,
    override val groupSpace: GroupSpace = GroupSpace.DEFAULT
) : TriggInit
