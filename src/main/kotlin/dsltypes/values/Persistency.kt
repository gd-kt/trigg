package dsltypes.values

import editor.objects.SimpleObject
import editor.objects.triggers.counters.PersistentTrigger
import editor.rawstring.property.NULL_GROUP
import generator.onlyGroup
import provider.ObjectManager

// TODO: Test

/**
 * Sets a [GDValue][WithIdGDValue] containing an itemID or a timerID to be persistent
 * @param gdValue the value to set as persistent
 * @param flag the behavior used to set the given [gdValue] as persistent
 * @return the given [gdValue]
 */
fun <Val : WithIdGDValue> ObjectManager<SimpleObject>.persistent(gdValue: Val, flag: PersistentType = PersistentType.ON_LEVEL_START): Val {
    if (flag == PersistentType.ON_LEVEL_START) {
        onlyGroup(NULL_GROUP) {
            this.add {
                PersistentTrigger(this.getNextPos().copy(x = 0f), gdValue.getItemID(this), true)
            }
        }
    } else {
        this.add {
            PersistentTrigger(this.getNextPos(), gdValue.getItemID(this), true)
        }
    }


    return gdValue
}

/**
 * Sets a [GDValue][WithIdGDValue] containing an itemID or a timerID to be persistent
 * @param gdValueGetter the value getter to set as persistent
 * @param flag the behavior used to set the given [GDValue][WithIdGDValue] as persistent
 * @return the given [GDValue][WithIdGDValue]
 */
inline fun <Val : WithIdGDValue> ObjectManager<SimpleObject>.persistent(gdValueGetter: () -> Val, flag: PersistentType = PersistentType.ON_LEVEL_START): Val =
    this.persistent(gdValueGetter(), flag)

enum class PersistentType {
    /**
     * Sets the itemID or timerID to be persistent ONLY WHEN
     * the persistent trigger gets triggered
     */
    ON_TRIGGER,
    /**
     * Sets the itemID or timerID to be persistent ON LEVEL START
     */
    ON_LEVEL_START
}