package provider

import Group
import TriggDsl
import editor.objects.GenericGdObject
import editor.objects.data.Pos
import editor.objects.triggers.misc.SpawnTrigger
import generator.TriggFunctionType

@TriggDsl
interface ObjectManager<in Obj : GenericGdObject>: ScopedObjectAdder<Obj>, GroupHandler

abstract class WrappedObjectManager<Obj : GenericGdObject>(private val wrappedContext: ObjectManager<Obj>) : ObjectManager<Obj> {
    override val nextFreeGroup: UInt
        get() = this.wrappedContext.nextFreeGroup

    override val nextFreeLabel: UInt
        get() = this.wrappedContext.nextFreeLabel

    override val acquiredRegisters: Set<UInt>
        get() = this.wrappedContext.acquiredRegisters

    override fun acquireGroup(): UInt =
        this.wrappedContext.acquireGroup()
    override fun acquireLabel(): UInt =
        this.wrappedContext.acquireLabel()

    override fun acquireRegister(): UInt =
        this.wrappedContext.acquireRegister()

    override fun releaseRegister(register: UInt) =
        this.wrappedContext.releaseRegister(register)

    override fun <T : Obj> add(obj: T): T =
        this.wrappedContext.add(obj)

    override fun spawn(group: Group?, delay: Float, spawner: ObjectManager<Obj>.() -> Unit): SpawnTrigger =
        this.wrappedContext.spawn(group, delay, spawner)

    override fun defineFunction(triggFunction: TriggFunctionType): UInt =
        this.wrappedContext.defineFunction(triggFunction)

    override fun spawnFunction(triggFunction: TriggFunctionType, canSpawnEarly: Boolean, delay: Float): SpawnTrigger =
        this.wrappedContext.spawnFunction(triggFunction)

    override fun getNextPos(): Pos =
        this.wrappedContext.getNextPos()
}