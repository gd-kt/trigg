package fr.geming400.gddotkt.trigg

import editor.objects.GenericGdObject

class TriggProvider internal constructor() {
    val trackedObjects = mutableListOf<GenericGdObject>()
    var labelSpace: ImmutableLabelSpace? = null
}

typealias TriggerProviderFunc = TriggProvider.() -> Unit

fun TriggProvider.labelSpace(labelSpaceSetter: LabelSpace.() -> Unit) {
    val labelSpace = LabelSpace()
    labelSpaceSetter(labelSpace)

    this.labelSpace = labelSpace.toImmutable()
}

fun trigg(triggEntryPoint: TriggerProviderFunc) {
    triggEntryPoint(TriggProvider())
}
