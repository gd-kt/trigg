package provider

import editor.objects.GenericGdObject
import editor.objects.data.Pos
import editor.objects.triggers.misc.SpawnTrigger

interface ObjectManager<Obj : GenericGdObject>: ScopedObjectAdder<Obj>, GroupHandler

abstract class WrappedObjectManager<Obj : GenericGdObject>(private val wrappedContext: ObjectManager<Obj>) : ObjectManager<Obj> {
    override val nextFreeGroup: UInt
        get() = this.wrappedContext.nextFreeGroup
    override val nextFreeLabel: UInt
        get() = this.wrappedContext.nextFreeLabel

    override fun acquireGroup(): UInt =
        this.wrappedContext.acquireGroup()
    override fun acquireLabel(): UInt =
        this.wrappedContext.acquireLabel()

    override fun <T : Obj> add(obj: T): T =
        this.wrappedContext.add(obj)

    override fun spawn(spawner: ObjectManager<Obj>.() -> Unit, delay: Float): SpawnTrigger =
        this.wrappedContext.spawn(spawner, delay)

    override fun getNextPos(): Pos =
        this.wrappedContext.getNextPos()
}