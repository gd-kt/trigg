package provider

import Group
import TriggDsl
import editor.objects.GenericGdObject
import editor.objects.SimpleObject
import editor.objects.data.Pos
import editor.objects.triggers.misc.SpawnTrigger
import editor.objects.triggers.setSpawned
import generator.GroupAdder

// TODO: spawn functions return SpawnTrigger
@TriggDsl
interface ScopedObjectAdder<Obj : GenericGdObject> {
    /**
     * Adds at "compile time" an object to the final output.
     * If the object is a trigger, it will be set as [spawned][editor.objects.triggers.TriggerObject.setSpawned]
     * and [multi triggered][editor.objects.triggers.TriggerObject.multiTriggered]
     * @return the created object. It is modifiable
     */
    fun <T : Obj> add(objectGetter: () -> T): T =
        this.add(objectGetter())

    /**
     * Adds at "compile time" an object to the final output.
     * If [obj] is a trigger, it will be set as [spawned][editor.objects.triggers.TriggerObject.setSpawned]
     * and [multi triggered][editor.objects.triggers.TriggerObject.multiTriggered]
     * @return the created object. It is modifiable
     */
    fun <T : Obj> add(obj: T): T

    /**
     * Adds at "compile time" objects to the final output.
     * If any the object is a trigger, it will be set as [spawned][editor.objects.triggers.TriggerObject.setSpawned]
     * and [multi triggered][editor.objects.triggers.TriggerObject.multiTriggered]
     */
    fun add(vararg objs: Obj) {
        this.add(objs.toList())
    }

    /**
     * Adds at "compile time" objects to the final output.
     * If any of the object is a trigger, it will be set as [spawned][editor.objects.triggers.TriggerObject.setSpawned]
     * and [multi triggered][editor.objects.triggers.TriggerObject.multiTriggered]
     */
    fun add(objs: Iterable<Obj>) =
        objs.forEach(::add)

    /**
     * Spawns a lambda after a given delay (which is `>=0`)
     * @param spawner what to spawn
     * @param delay the time after which the [lambda][spawner] will get spawned
     */
    fun spawn(spawner: ObjectManager<Obj>.() -> Unit, delay: Float)

    /**
     * Spawns a lambda instantly (with a delay of `0`)
     * @param spawner what to spawn
     */
    fun spawn(spawner: ObjectManager<Obj>.() -> Unit) =
        this.spawn(spawner, 0f)

    /**
     * Gets the pos to place a trigger.
     * This is not needed but is more for not
     * cluttering every objects around a single point
     * @return the pos where to place the next object
     */
    fun getNextPos(): Pos
}

/**
 * Spawns a specific group
 * @param group the group to spawn
 * @return the created [SpawnTrigger]
 */
fun ScopedObjectAdder<in SpawnTrigger>.spawn(group: Group, delay: Float = 0f): SpawnTrigger =
    this.add {
        SpawnTrigger(this.getNextPos(), group.value, delay)
    }

fun ScopedObjectAdder<in SimpleObject>.add(groupAdder: GroupAdder) =
    this.add(groupAdder.collectedObjects)
