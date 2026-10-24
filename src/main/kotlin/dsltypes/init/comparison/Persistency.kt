package dsltypes.init.comparison

import editor.objects.SimpleObject
import editor.objects.triggers.counters.PersistentTrigger
import generator.onlyGroup
import provider.ObjectManager

/**
 * Sets a [GDValue][WithIdGDValue] containing an itemID or a timerID to be persistent
 */
fun <Val : WithIdGDValue> ObjectManager<SimpleObject>.persistent(gdValue: Val, flag: PersistentType = PersistentType.ON_LEVEL_START): Val {
    if (flag == PersistentType.ON_LEVEL_START) {
        onlyGroup(0u) {
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