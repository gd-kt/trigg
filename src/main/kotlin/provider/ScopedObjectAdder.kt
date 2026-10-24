package provider

import TriggDsl
import editor.objects.GenericGdObject
import editor.objects.SimpleObject
import editor.objects.data.Pos
import generator.GroupAdder

@TriggDsl
interface ScopedObjectAdder<Obj: GenericGdObject> {
    /**
     * Adds at "compile time" an object to the final output
     * @return the created object. It is modifiable
     */
    fun <T: Obj> add(objectGetter: () -> T): T

    /**
     * Adds at "compile time" an object to the final output
     * @return the created object. It is modifiable
     */
    fun <T: Obj> add(obj: T): T

    /**
     * Adds at "compile time" objects to the final output
     */
    fun add(vararg objs: Obj) {
        this.add(objs.toList())
    }

    /**
     * Adds at "compile time" objects to the final output
     */
    fun add(objs: Iterable<Obj>)

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
    fun spawn(spawner: ObjectManager<Obj>.() -> Unit) = this.spawn(spawner, 0f)

    /**
     * Gets the pos to place a trigger.
     * This is not needed but is more for not
     * cluttering every objects around a single point
     * @return the pos where to place the next object
     */
    fun getNextPos(): Pos
}

fun ScopedObjectAdder<SimpleObject>.add(groupAdder: GroupAdder) {
    this.add(groupAdder.collectedObjects)
}
