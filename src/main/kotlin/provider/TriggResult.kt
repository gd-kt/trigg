package provider

import editor.wsliveeditor.AsyncWSLiveEditor
import editor.wsliveeditor.WSLiveEditor

/**
 * The result of a [TriggProvider]
 * @see TriggProvider
 */
data class TriggResult(
    val triggProvider: TriggProvider
) {
    val rawString: String
        get() = this.triggProvider.asRawString()
}

fun WSLiveEditor.addTriggResult(triggResult: TriggResult) =
    this.addObjects(triggResult.rawString)

fun AsyncWSLiveEditor.addTriggResult(triggResult: TriggResult) =
    this.addObjects(triggResult.rawString)
