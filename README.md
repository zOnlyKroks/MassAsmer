---

# MassASM API

A lightweight Fabric-based API for registering on-the-fly ASM bytecode transformers across all game classes.
MassASM extends Fabric's `GameTransformer`, applying custom transformations *after* Fabric's Minecraft patches but *before* Mixins — important for compatibility reasons.

[![](https://jitpack.io/v/zOnlyKroks/MassAsmer.svg)](https://jitpack.io/#zOnlyKroks/MassAsmer)

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Key Components](#key-components)
3. [Filters Utility](#filters-utility)
4. [Registering Transformers](#registering-transformers)
5. [Entrypoints](#entrypoints)
6. [Examples](#examples)
7. [Error Handling](#error-handling)
8. [License](#license)

---

## Getting Started

1. **Include** the `massasmer` library as a dependency in your Fabric mod.
2. **Implement** a `PreLaunchEntrypoint` that injects MassASM into the game's transformer pipeline.
3. **Register** your transformers by providing:

   * a **unique name**,
   * a **`TransformerFilter`** from the `Filters` utility,
   * a **transformer implementation**.

---

## Key Components

| Class                       | Description                                                                                  |
| --------------------------- | -------------------------------------------------------------------------------------------- |
| `MassASMTransformer`        | Extends Fabric's `GameTransformer`; manages and applies all registered MassASM transformers. |
| `TransformerFilter`         | Interface for matching class names; use implementations from `Filters`.                      |
| `Filters`                   | Factory for creating and combining common `TransformerFilter` instances.                     |
| `MassasmerPreLaunch`        | Fabric `PreLaunchEntrypoint` to inject `MassASMTransformer` during boot.                     |
| `LoggerWrapper`             | Lightweight logger wrapper used internally and in injected transformers.                     |
| `InternalMassAsmEntrypoint` | Example registration for a simple visitor-based transformer.                                 |

---

## Filters Utility

Use the `Filters` factory to create and compose filters:

```java
import de.zonlykroks.massasmer.filter.Filters;
import de.zonlykroks.massasmer.filter.api.TransformerFilter;
```

### Name-Based Filters

| Method                       | Description                         |
| ---------------------------- | ----------------------------------- |
| `Filters.all()`              | Matches *all* class names.          |
| `Filters.none()`             | Matches *no* class names.           |
| `Filters.exact(name)`        | Exact class name match.             |
| `Filters.startsWith(prefix)` | Class names starting with `prefix`. |
| `Filters.endsWith(suffix)`   | Class names ending with `suffix`.   |
| `Filters.contains(substr)`   | Class names containing `substr`.    |
| `Filters.regex(pattern)`     | Class names matching the regex.     |

### Structure-Based Filters

| Method                                  | Description                                      |
| --------------------------------------- | ------------------------------------------------ |
| `Filters.hasAnnotation(annotationClass)`| Classes with the specified annotation.           |
| `Filters.lacksAnnotation(annotationClass)`| Classes without the specified annotation.      |
| `Filters.implementsInterface(interfaceClass)`| Classes implementing the specified interface.|
| `Filters.doesNotImplementInterface(interfaceClass)`| Classes not implementing the specified interface.|
| `Filters.extendsClass(superClass)`      | Classes extending the specified superclass.      |
| `Filters.doesNotExtendClass(superClass)`| Classes not extending the specified superclass.  |

### Composition Filters

| Method                       | Description                         |
| ---------------------------- | ----------------------------------- |
| `Filters.and(f1, f2)`        | Logical AND of two filters.         |
| `Filters.or(f1, f2)`         | Logical OR of two filters.          |
| `Filters.not(f)`             | Negates the given filter.           |

---

## Registering Transformers

You must provide:

* a **name** (string),
* a **`TransformerFilter`** from the `Filters` utility,
* a **transformer** implementation.

There are three registration types:

### Raw Bytecode Transformers

```java
MassASMTransformer.register(
    "unique-name",
    Filters.startsWith("com.example"),
    (className, classBytes) -> {
        // return a VisitorProvider
    }
);
```

### ClassNode Transformers

```java
MassASMTransformer.registerNodeTransformer(
    "node-transformer",
    Filters.exact("net.minecraft.client.Minecraft"),
    (className, classNode) -> {
        // transform the ClassNode here
        return classNode;
    }
);
```

### Visitor-Based Transformers

```java
MassASMTransformer.registerVisitor(
    "visitor-transformer",
    Filters.contains("Minecraft"),
    (className, nextVisitor) -> new ClassVisitor(Opcodes.ASM9, nextVisitor) {
        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            if ("<init>".equals(name)) {
                return new AdviceAdapter(api, mv, access, name, descriptor) {
                    @Override
                    protected void onMethodEnter() {
                        // Inject bytecode at method entry
                    }
                };
            }
            return mv;
        }
    }
);
```

---

## Entrypoints

### MassASM Entrypoint (`mass-asm`)

Implement a `Runnable` class to register your transformers, then declare it in `fabric.mod.json`:

```java
public class InternalMassAsmEntrypoint implements Runnable {
    @Override
    public void run() {
        // Register transformers here
    }
}
```

In `fabric.mod.json`:

```json
{
  "entrypoints": {
    "mass-asm": [
      "de.zonlykroks.massasmer.InternalMassAsmEntrypoint"
    ]
  }
}
```

---

## Examples

### Basic Example

Here's a usage example leveraging `Filters` in an internal entrypoint:

```java
public class InternalMassAsmEntrypoint implements Runnable {
    @Override
    public void run() {
        MassASMTransformer.registerVisitor(
            "massasm-internal-inject-init-stdout",
            Filters.exact(
                FabricLauncherBase.getLauncher().isDevelopment()
                    ? "net.minecraft.client.Minecraft"
                    : "net.minecraft.client.main.Main$2"
            ),
            (className, nextVisitor) -> new CreateTitlePrintTransformer(Opcodes.ASM9, nextVisitor, className)
        );
    }
}
```

This injects a log message into the target constructor when running in development or production respectively.

### Structure-Based Filter Examples

Target all serializable classes except tests:

```java
TransformerFilter filter = Filters.and(
    Filters.implementsInterface(Serializable.class),
    Filters.not(Filters.contains("Test"))
);
```

Target entities that extend a specific base class:

```java
TransformerFilter entityFilter = Filters.and(
    Filters.startsWith("net.minecraft.world.entity"),
    Filters.extendsClass(LivingEntity.class)
);
```

Target deprecated methods in client classes:

```java
TransformerFilter deprecatedFilter = Filters.and(
    Filters.startsWith("net.minecraft.client"),
    Filters.hasAnnotation(Deprecated.class)
);
```

---

## Error Handling

* If MassASM fails to inject due to Fabric changes, a stack trace is printed.
* If a transformer throws during transformation, an `UnrecoverableMassASMRuntimeError` is thrown, crashing the game.

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for full license text.

---