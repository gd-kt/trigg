package dsltypes.comparison

import Group.Companion.group
import OldEntrypointTriggProvider
import dsltypes.values.*
import editor.objects.SimpleObject
import editor.objects.data.Pos
import editor.objects.triggers.counters.ItemCompareComparisonType
import editor.objects.triggers.counters.ItemCompareTrigger
import editor.objects.triggers.counters.compare
import editor.objects.triggers.misc.SpawnTrigger
import editor.objects.triggers.misc.TimewarpTrigger
import editor.objects.triggers.objects.ColorTrigger
import editor.objects.triggers.objects.SpecialColors
import editor.rawstring.property.NULL_GROUP
import editor.rawstring.property.getOrThrow
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import provider.TriggProvider
import java.awt.Color
import kotlin.random.Random
import kotlin.random.nextUInt

private class ComparisonTests {
    fun TriggProvider.getComp(comparisonCreator: ComparisonCreator): Comparison =
        comparisonCreator(this)

    @Test
    fun inliningTest() {
        val a = const(Random.nextUInt())
        val b = constFloat(Random.nextFloat())

        val ctx = TriggProvider()
        for (compType in ItemCompareComparisonType.entries) {
            val comp = ctx.getComp(compType.getComparison(a, b))
            Assertions.assertEquals(compType, comp.operator)

            val inlinedBranch = Comparison.InlinedBranch.fromEvaluation(
                compType.compare(a.value.toDouble(), b.value.toDouble())
            )

            Assertions.assertEquals(inlinedBranch, comp.inlinedBranch)
        }
    }

    @Test
    fun comparisonOperatorsTest() {
        val a = item(5u)
        val b = const(Random.nextUInt())

        val ctx = TriggProvider()
        Assertions.assertEquals(ItemCompareComparisonType.EQUAL, ctx.getComp(a equalsTo b).operator)
        Assertions.assertEquals(ItemCompareComparisonType.NOT_EQUAL, ctx.getComp(a notEqualTo b).operator)
        Assertions.assertEquals(ItemCompareComparisonType.GREATER_THAN, ctx.getComp(a greaterThan b).operator)
        Assertions.assertEquals(ItemCompareComparisonType.GREATER_THAN_EQUAL, ctx.getComp(a greaterThanEqual b).operator)
        Assertions.assertEquals(ItemCompareComparisonType.LESS_THAN, ctx.getComp(a lessThan b).operator)
        Assertions.assertEquals(ItemCompareComparisonType.LESS_THAN_EQUAL, ctx.getComp(a lessThanEqual b).operator)

        Assertions.assertEquals(ItemCompareComparisonType.EQUAL, ctx.getComp(b equalsTo a).operator)
        Assertions.assertEquals(ItemCompareComparisonType.NOT_EQUAL, ctx.getComp(b notEqualTo a).operator)
        Assertions.assertEquals(ItemCompareComparisonType.GREATER_THAN.opposite(), ctx.getComp(b greaterThan a).operator)
        Assertions.assertEquals(ItemCompareComparisonType.GREATER_THAN_EQUAL.opposite(), ctx.getComp(b greaterThanEqual a).operator)
        Assertions.assertEquals(ItemCompareComparisonType.LESS_THAN.opposite(), ctx.getComp(b lessThan a).operator)
        Assertions.assertEquals(ItemCompareComparisonType.LESS_THAN_EQUAL.opposite(), ctx.getComp(b lessThanEqual a).operator)
    }
}

private class CompBranchesBuilderTests {
    @AfterEach
    fun afterEach() {
        TriggProvider.INSTANCE = null
    }

    @Test
    fun thenBranchTest() {
        val ctx = OldEntrypointTriggProvider()
        TriggProvider.INSTANCE = ctx

        val compNoInline = ctx.comp(attempts() greaterThanEqual const(15)) {
            then {
                add { SimpleObject(1u, Pos.ZERO) }
                add { TimewarpTrigger(Pos.ZERO) }
            }
        }

        Assertions.assertNotNull(compNoInline.createObject(ctx))

        Assertions.assertFalse {
            ctx.addedObjects
                .filterIsInstance<SimpleObject>()
                .all { it.groups.contains(ctx.init.groupSpace.getEntrypoint(ctx).value) }
        }

        Assertions.assertEquals(2 + 1, ctx.addedObjects.size)
        Assertions.assertEquals(
            1,
            ctx.addedObjects
                .filterIsInstance<ItemCompareTrigger>()
                .size
        )

        Assertions.assertEquals(NULL_GROUP, compNoInline.obj.failedSpawnID.getOrThrow())
        Assertions.assertEquals(1u, compNoInline.obj.succeededSpawnID.getOrThrow())
    }

    @Test
    fun inlinedThenBranchTest() {
        val ctx = OldEntrypointTriggProvider()
        TriggProvider.INSTANCE = ctx

        val compInline: Comparison
        ctx.entrypoint(execute = false) {
            compInline = comp(const(8) equalsTo const(8)) {
                then {
                    add { SimpleObject(1u, Pos.ZERO) }
                    add { TimewarpTrigger(Pos.ZERO) }
                }
            }
        }

        Assertions.assertNull(compInline.createObject(ctx))

        Assertions.assertTrue {
            ctx.addedObjects
                .filterIsInstance<SimpleObject>()
                .all { it.groups.contains(ctx.init.groupSpace.getEntrypoint(ctx).value) }
        }

        Assertions.assertEquals(2, ctx.addedObjects.size)
        Assertions.assertEquals(
            0,
            ctx.addedObjects
                .filterIsInstance<ItemCompareTrigger>()
                .size
        )

        Assertions.assertEquals(NULL_GROUP, compInline.obj.failedSpawnID.getOrThrow())
        Assertions.assertEquals(NULL_GROUP, compInline.obj.succeededSpawnID.getOrThrow())
    }

    @Test
    fun inlinedThenBranchOtherValueTest() {
        val ctx = OldEntrypointTriggProvider()
        TriggProvider.INSTANCE = ctx

        val compInline: Comparison
        ctx.entrypoint(execute = false) {
            compInline = comp(attempts() greaterThanEqual attempts()) {
                then {
                    add { SimpleObject(1u, Pos.ZERO) }
                    add { TimewarpTrigger(Pos.ZERO) }
                }
            }
        }

        Assertions.assertNull(compInline.createObject(ctx))

        Assertions.assertTrue {
            ctx.addedObjects
                .filterIsInstance<SimpleObject>()
                .all { it.groups.contains(ctx.init.groupSpace.getEntrypoint(ctx).value) }
        }

        Assertions.assertEquals(2, ctx.addedObjects.size)
        Assertions.assertEquals(
            0,
            ctx.addedObjects
                .filterIsInstance<ItemCompareTrigger>()
                .size
        )

        Assertions.assertEquals(NULL_GROUP, compInline.obj.failedSpawnID.getOrThrow())
        Assertions.assertEquals(NULL_GROUP, compInline.obj.succeededSpawnID.getOrThrow())
    }

    @Test
    fun elseBranchTest() {
        val ctx = OldEntrypointTriggProvider()
        TriggProvider.INSTANCE = ctx

        val compNoInline = ctx.comp(attempts() greaterThanEqual const(15)) {
            then {
                add { SimpleObject(1u, Pos.ZERO) }
                add { TimewarpTrigger(Pos.ZERO) }
            } fail {
                add { SimpleObject(1u, Pos.ZERO) }
                add { TimewarpTrigger(Pos.ZERO) }
                add { SpawnTrigger(Pos.ZERO) }
            }
        }

        Assertions.assertEquals(2 + 3 + 1, ctx.addedObjects.size)
        Assertions.assertEquals(
            1,
            ctx.addedObjects
                .filterIsInstance<ItemCompareTrigger>()
                .size
        )

        Assertions.assertEquals(2u, compNoInline.obj.failedSpawnID.getOrThrow())
        Assertions.assertEquals(1u, compNoInline.obj.succeededSpawnID.getOrThrow())
    }

    @Test
    fun inlineElseBranchTest() {
        val ctx = OldEntrypointTriggProvider()
        TriggProvider.INSTANCE = ctx

        val compInline = ctx.comp(const(12) greaterThanEqual const(15)) {
            then {
                add { SimpleObject(1u, Pos.ZERO) }
                add { TimewarpTrigger(Pos.ZERO) }
            } fail {
                add { SimpleObject(1u, Pos.ZERO) }
                add { TimewarpTrigger(Pos.ZERO) }
                add { SpawnTrigger(Pos.ZERO) }
            }
        }

        Assertions.assertEquals(3, ctx.addedObjects.size)
        Assertions.assertEquals(
            0,
            ctx.addedObjects
                .filterIsInstance<ItemCompareTrigger>()
                .size
        )

        Assertions.assertEquals(NULL_GROUP, compInline.obj.failedSpawnID.getOrThrow())
        Assertions.assertEquals(NULL_GROUP, compInline.obj.succeededSpawnID.getOrThrow())
    }

    @Test
    fun inlineElseBranchOtherValueTest() {
        val ctx = OldEntrypointTriggProvider()
        TriggProvider.INSTANCE = ctx

        val compInline = ctx.comp(attempts() lessThan attempts()) {
            then {
                add { SimpleObject(1u, Pos.ZERO) }
                add { TimewarpTrigger(Pos.ZERO) }
            } fail {
                add { SimpleObject(1u, Pos.ZERO) }
                add { TimewarpTrigger(Pos.ZERO) }
                add { SpawnTrigger(Pos.ZERO) }
            }
        }

        Assertions.assertEquals(3, ctx.addedObjects.size)
        Assertions.assertEquals(
            0,
            ctx.addedObjects
                .filterIsInstance<ItemCompareTrigger>()
                .size
        )

        Assertions.assertEquals(NULL_GROUP, compInline.obj.failedSpawnID.getOrThrow())
        Assertions.assertEquals(NULL_GROUP, compInline.obj.succeededSpawnID.getOrThrow())
    }

    @Test
    fun elseifBranchTest() {
        val ctx = OldEntrypointTriggProvider()
        TriggProvider.INSTANCE = ctx

        val compNoInline = ctx.comp(attempts() greaterThanEqual const(15)) {
            then {
                add { SimpleObject(1u, Pos.ZERO) }
                add { TimewarpTrigger(Pos.ZERO) }
            }

            elseif(attempts() equalsTo const(4)) {
                add { TimewarpTrigger(Pos.ZERO) }
                add { ColorTrigger(Pos.ZERO, Color.RED, SpecialColors.BACKGROUND.colorID) }
            }

            fail {
                add { SimpleObject(1u, Pos.ZERO) }
                add { TimewarpTrigger(Pos.ZERO) }
                add { SpawnTrigger(Pos.ZERO) }
            }
        }

        Assertions.assertEquals(2 + 2 + 3 + 2, ctx.addedObjects.size)
        Assertions.assertEquals(
            2,
            ctx.addedObjects
                .filterIsInstance<ItemCompareTrigger>()
                .size
        )

        Assertions.assertNotEquals(2u, compNoInline.obj.failedSpawnID.getOrThrow())
        Assertions.assertEquals(1u, compNoInline.obj.succeededSpawnID.getOrThrow())
    }

    @Test
    fun branchSpecificGroupTest() {
        val ctx = OldEntrypointTriggProvider()
        TriggProvider.INSTANCE = ctx

        val comp = ctx.comp(attempts() greaterThanEqual const(8)) {
            then(6.group)
            fail(7.group)
        }

        Assertions.assertEquals(6u, comp.obj.succeededSpawnID.getOrThrow())
        Assertions.assertEquals(7u, comp.obj.failedSpawnID.getOrThrow())
    }


    @Test
    fun branchesSafeguardTest() {
        val ctx = TriggProvider()
        var builder = ComparisonBranchesBuilder(ctx, const(0) equalsTo const(0))

        fun resetBuilder() {
            builder = ComparisonBranchesBuilder(ctx, const(0) equalsTo const(0))
        }

        Assertions.assertDoesNotThrow {
            builder.then {  }
        }

        resetBuilder()
        Assertions.assertDoesNotThrow {
            builder.then {  }
            builder.fail {  }
        }

        resetBuilder()
        Assertions.assertDoesNotThrow {
            builder.then {  }
            builder.elseif(const(0) equalsTo const(0)) {  }
            builder.elseif(const(0) equalsTo const(0)) {  }
            builder.elseif(const(0) equalsTo const(0)) {  }
            builder.elseif(const(0) equalsTo const(0)) {  }
            builder.elseif(const(0) equalsTo const(0)) {  }
            builder.fail {  }
        }


        resetBuilder()
        Assertions.assertThrows(IllegalStateException::class.java) {
            builder.then {  }
            builder.then {  }
        }

        resetBuilder()
        Assertions.assertThrows(IllegalStateException::class.java) {
            builder.then {  }
            builder.fail {  }
            builder.fail {  }
        }

        resetBuilder()
        Assertions.assertThrows(IllegalStateException::class.java) {
            builder.fail {  }
        }

        resetBuilder()
        Assertions.assertThrows(IllegalStateException::class.java) {
            builder.elseif(const(0) equalsTo const(0)) {  }
        }

        resetBuilder()
        Assertions.assertThrows(IllegalStateException::class.java) {
            builder.then {  }
            builder.fail {  }
            builder.elseif(const(0) equalsTo const(0)) {  }
        }
    }
}
