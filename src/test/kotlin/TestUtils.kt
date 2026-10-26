import Group.Companion.group
import dsltypes.init.MutableTriggInit
import dsltypes.init.space.GroupSpace
import dsltypes.init.space.MutableGroupSpace
import editor.objects.propertycontainers.TriggerProperties
import provider.TriggProvider

/**
 * Inits the trigg providers' init thingies, but sets the entrypoint to `9998`
 * @see TriggProvider.init
 */
internal fun TriggProvider.testsInit(initGetter: MutableTriggInit.() -> Unit) {
    this.init {
        initGetter()

        val groupSpace = this.groupSpace
        if (groupSpace is MutableGroupSpace && groupSpace.entrypoint == GroupSpace.DEFAULT.entrypoint)
            groupSpace.entrypoint = (TriggerProperties.MAX_GROUPS - 1u).group
    }
}