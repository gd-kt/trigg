import Group.Companion.group
import dsltypes.init.MutableTriggInit
import dsltypes.init.space.GroupSpace
import dsltypes.init.space.groupSpace
import editor.objects.propertycontainers.TriggerProperties
import provider.TriggProvider

/**
 * Inits the trigg providers' init thingies, but sets the entrypoint to `9998`
 * @see TriggProvider.init
 */
internal fun TriggProvider.oldEntrypointInit(initGetter: MutableTriggInit.() -> Unit = {}) {
    val triggInit = MutableTriggInit(this)
    initGetter(triggInit)

    this.init {
        groupSpace {
            if (triggInit.groupSpace.entrypoint == GroupSpace.DEFAULT.entrypoint)
                entrypoint = (TriggerProperties.MAX_GROUPS - 1u).group

//            baseOffset = triggInit.groupSpace.baseOffset
//            allocatedGroups = triggInit.groupSpace.allocatedGroups
            groupRange = triggInit.groupSpace.groupRange
        }

        commonGroup = triggInit.commonGroup
        labelSpace = triggInit.labelSpace
    }
}

@Suppress("TestFunctionName")
internal fun OldEntrypointTriggProvider(): TriggProvider =
    TriggProvider().apply {
        oldEntrypointInit()
    }
