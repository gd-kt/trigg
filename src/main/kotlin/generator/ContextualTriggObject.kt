package generator

import editor.objects.SimpleObject
import provider.TriggProvider

/**
 * Represents a class related to `Trigg`.
 * It has a [context] and can create an object of type [T]
 */
interface ContextualTriggObject<T : SimpleObject> : TriggObject<T> {
    val context: TriggProvider
}