package generator

import editor.objects.GenericGdObject
import editor.objects.SimpleObject
import provider.ObjectManager
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType

typealias TriggFunctionType = KFunction<Unit>

/**
 * A [TriggFunction] is a function that can be spawned by any [provider.ScopedObjectAdder].
 * TriggFunctions are used in 2 steps:
 * - Definition
 * - Actually calling it
 *
 * ## Definition
 *
 * Like in any programming language, a [TriggFunction] needs to be defined.
 * However, it can still be called before its definition.
 *
 * A [TriggFunction] must exactly have the signature:
 * ```kotlin
 * @TriggFunction
 * fun myFunc(ctx: ScopedObjectAdder<SimpleObject>)
 * ```
 *
 * It is defined like so:
 * ```kotlin
 * @TriggFunction
 * fun myFunc(ctx: ScopedObjectAdder<SimpleObject>) {
 *    ctx.apply {
 *        add { ColorTrigger(getNextPos(), Color.GREEN, SpecialColors.BACKGROUND.colorID) }
 *        // ...
 *    }
 * }
 *
 * // It works on any ScopedObjectAdder<*> impl
 * trigg {
 *     defineFunction(::myFunc)
 * }
 * ```
 *
 * ## Calling it
 *
 * Calling it is as easy as defining it:
 *
 * ```kotlin
 * trigg {
 *     defineFunction(::myFunc)
 *
 *     entrypoint {
 *         // It works on any ScopedObjectAdder<*> impl
 *         spawnFunction(::myFunc)
 *
 *         // You can also add some delay
 *         spawnFunction(::myFunc, delay = 1.75f)
 *     }
 * }
 * ```
 *
 * Although, if you spawn a function without defining it, an [IllegalStateException]
 * exception will be thrown.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(allowedTargets = [AnnotationTarget.FUNCTION])
annotation class TriggFunction {
    companion object {
        fun validate(triggFunction: TriggFunctionType, objectClazz: KClass<out GenericGdObject> = SimpleObject::class) {
            require(triggFunction.parameters.size == 1) { "A TriggFunction only accepts 1 parameter of type ObjectManger<SimpleObject> !" }

            val simpleObjectType = objectClazz.starProjectedType
            val objectManagerType = ObjectManager::class.createType(listOf(KTypeProjection(KVariance.INVARIANT, simpleObjectType)))
            require(triggFunction.parameters.first().type == objectManagerType) { "A TriggFunction only accepts 1 parameter of type ObjectManger<SimpleObject> !" }

            require(triggFunction.annotations.contains(TriggFunction())) { "A TriggFunction must be annotated with @TriggFunction" }
        }
    }
}
