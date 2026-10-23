package generator

import editor.objects.GenericGdObject
import provider.TriggProvider

/**
 * Represents a class related to `Trigg`.
 * It has a [context] and can create an object of type [T]
 */
interface ContextualTriggObject<T : GenericGdObject> : TriggObject<T> {
    val context: TriggProvider
}