package generator

import editor.objects.GenericGdObject
import provider.TriggProvider

/**
 * Represents a class related to `Trigg`.
 * It has a [context] and can create an object of type [Obj]
 */
interface TriggObject<Obj : GenericGdObject> {
    /**
     * Creates the object linked to this [TriggObject]
     */
    fun createObject(context: TriggProvider): Obj? = null

    fun createAllObjects(context: TriggProvider): Collection<GenericGdObject> {
        val obj = this.createObject(context)
        return if (obj == null)
            emptyList()
        else
            listOf(obj)
    }
}