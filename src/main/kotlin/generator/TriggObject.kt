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

    /**
     * Creates all the objects of this [TriggObject].
     * However, they are all added to the given [context]
     */
    fun createAllObjects(context: ObjectManager<SimpleObject>) {
        val obj = this.createObject(context)
        if (obj != null)
            context.add(obj)
    }
}