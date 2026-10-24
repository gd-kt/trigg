package generator

import editor.objects.SimpleObject
import provider.ObjectManager

/**
 * Represents a class related to `Trigg`.
 * It has a [context] and can create an object of type [Obj]
 */
interface TriggObject<Obj : SimpleObject> {
    /**
     * Creates the object linked to this [TriggObject]
     */
    fun createObject(context: ObjectManager<SimpleObject>): Obj? = null

    fun createAllObjects(context: ObjectManager<SimpleObject>): Collection<SimpleObject> {
        val obj = this.createObject(context)
        return if (obj == null)
            emptyList()
        else
            listOf(obj)
    }
}