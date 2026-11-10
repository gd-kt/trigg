<div align="center">
    <img src="https://github.com/gd-kt/docs/raw/main/docs/public/logos/trigg/logo_title.png" height=50% width=50% alt="trigg logo">
</div>

# Trigg

[![Jitpack Badge](https://jitpack.io/v/gd-kt/trigg.svg)](https://jitpack.io/#gd-kt/trigg)

Trigg is a [spwn](https://github.com/Spu7Nix/SPWN-language) inspired kotlin DSL.
It is dependent on [gd.kt](https://github.com/gd-kt/gd.kt).

It allows, like [spwn](https://github.com/Spu7Nix/SPWN-language), to make complex trigger stuff, but in a way where they mostly are abstracted away.
However, unlike [spwn](https://github.com/Spu7Nix/SPWN-language) all objects are properly typed and type safe.

> [!WARNING]
> `trigg`'s documentation is not finished yet !

## Installation

You can install this lib via JitPack:
```kts
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.gd-kt:trigg:<version tag>")
    // gd.kt is technichally optional but you won't be able
    // to do much without it.
    // It provides the api to create objects
    implementation("com.github.gd-kt:gd.kt:<gd.kt version tag>")
}
```

## Usage

### Creating a Trigg instance

The `TriggProvider` class is the base class for all Trigg projects.
It can be created like so:
```kotlin
trigg {
    // ...
}
```

### Adding objects

You can add objects using [gd.kt](https://github.com/gd-kt/gd.kt)'s object system:

```kotlin
add { SimpleObject(1u, Pos.ZERO) }
```

### Spawning

Spawning groups can be done with the `spawn` function:
```kotlin
// Spawns the content of the spawn clause
spawn {
    add { ToggleTrigger(Pos.ZERO, 12u) }
}
```

```kotlin
// Spawns the group 5
// .group is an extension of Int and UInt
spawn(5.group)
```

```kotlin
@TriggFunction
fun myTriggFunction(ctx: ObjectManager<SimpleObject>) {
    ctx.apply {
        add { ColorTrigger(getNextPos(), Color.GREEN, SpecialColors.BACKGROUND.colorID) }
    }
}

// We define a TriggFunction and then spawn it
// The defining step is necessary
defineFunction(::trigFunction)
spawnFunction(::myTriggFunction)
```

### Comparisons

Trigg supports comparisons:
```kotlin
comp(item(5u) greaterThan const(8)) {
    then {
        spawnFunction(::myTriggFunction)
    } fail {
        add { ToggleTrigger(Pos.ZERO, 12u) }
    }
}
```

`else if` statements are also supported:
```kotlin
comp(item(5u) greaterThan const(8)) {
    then {
        spawnFunction(::myTriggFunction)
    }
    
    elseif(item(6u) equalTo attempts()) {
        add { ToggleTrigger(Pos.ZERO, 11u) }
        add { ToggleTrigger(Pos.ZERO, 12u) }
    }

    fail {
        add { ToggleTrigger(Pos.ZERO, 12u) }
    }
}
```

### Loops

Trigg also contains `for loops` and `while loops`:
```kotlin
forLoop(item(8u), { it.add(5) }, { it lessThan const(10) }) {
    onFinish {
        add { ToggleTrigger(getNextPos(), 9u) }
    }

    // This gets executed on each iteration
    spawnFunction(::triggFunction)
}
```

```kotlin
whileLoop({ item(1u) lessThan const(5) }) {
    onFinish {
        add { ToggleTrigger(getNextPos(), 9u) }
    }

    // This gets executed on each iteration
    spawnFunction(::triggFunction)
}
```

### Retrieving the result

After finishing your trigg script, you must get the final result at the end somehow:
```kotlin
val result = trigg {
    // ...
}
```

then you can use the [WSLiveEditor](https://geode-sdk.org/mods/iandyhd3.wsliveeditor) integration to
add the final result directly into the game:
```kotlin
WSLiveEditor().use {
    it.addTriggResult(result)
}
```

> [!TIP]
> Learn more about its integration on [gd.kt's docs](https://gd-kt.github.io/docs/gdkt/editor-api/wsliveeditor.html) !
