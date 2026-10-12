import editor.objects.GenericGdObject

/**
 * Represents a class related to `Trigg`.
 * It has a [context] and can create an object of type [T]
 */
interface TriggObject<T : GenericGdObject> {
    val context: TriggProvider

    /**
     * Creates the object linked to this [TriggObject]
     */
    fun createObject(): T
}