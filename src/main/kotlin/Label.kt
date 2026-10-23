import editor.objects.GenericGdObject
import editor.objects.triggers.counters.PersistentTrigger
import generator.ContextualTriggObject
import provider.TriggProvider

class Label internal constructor(override val context: TriggProvider, val addVisualCounter: Boolean = false, val owner: Owner):
    ContextualTriggObject<GenericGdObject> {
    val itemID: UInt = TODO()

    override fun createObject(context: TriggProvider) = TODO("Not yet implemented")
}

fun TriggProvider.persistent(labelGetter: () -> Label): Label {
    val label = labelGetter()
    this.add { PersistentTrigger(this.getNextPos(), label.itemID, true) }

    return label
}
