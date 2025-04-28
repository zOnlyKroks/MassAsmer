---

# MassASM API

A lightweight Fabric-based API for registering on-the-fly ASM bytecode transformers across all game classes.  
MassASM extends Fabric's `GameTransformer`, applying custom transformations *after* Fabric's Minecraft patches but *before* Mixins — important for compatibility reasons.

[![](https://jitpack.io/v/zOnlyKroks/MassAsmer.svg)](https://jitpack.io/#zOnlyKroks/MassAsmer)

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Key Components](#key-components)
3. [Registering Transformers](#registering-transformers)
   - [Raw Bytecode Transformers](#raw-bytecode-transformers)
   - [ClassNode Transformers](#classnode-transformers)
   - [Visitor-Based Transformers](#visitor-based-transformers)
4. [Entrypoints](#entrypoints)
5. [Examples](#examples)
6. [Error Handling](#error-handling)
7. [License](#license)

---

## Getting Started

1. **Include** the `massasmer` library as a dependency in your Fabric mod.
2. **Implement** a `PreLaunchEntrypoint` that injects MassASM into the game's transformer pipeline.
3. **Register** your transformers by providing:
   - a **unique name**,
   - a **TransformerFilter**,
   - a **transformer implementation**.

---

## Key Components

| Class                        | Description |
|-------------------------------|-------------|
| `MassASMTransformer`          | Extends Fabric's `GameTransformer`; manages and applies all registered MassASM transformers. |
| `TransformerFilter`           | Interface for matching class names (e.g., `NamePatternFilter`, `CompositeFilter`, `EmptyFilter`). |
| `MassasmerPreLaunch`          | Fabric `PreLaunchEntrypoint` to inject `MassASMTransformer` during boot. |
| `LoggerWrapper`               | Lightweight logger wrapper used internally and in injected transformers. |
| `InternalMassAsmEntrypoint`    | Example registration for a simple visitor-based transformer. |

---

## Registering Transformers

You must provide:
- a **name** (string),
- a **`TransformerFilter`** (not a simple predicate),
- a **transformer**.

There are three ways to register:

### Raw Bytecode Transformers

```java
MassASMTransformer.register(
    "unique-name",
    new NamePatternFilter("com.example", true, false, false, false),
    (className, classBytes) -> {
        // return a VisitorProvider
    }
);
```

### ClassNode Transformers

```java
MassASMTransformer.registerNodeTransformer(
    "node-transformer",
    new NamePatternFilter("net.minecraft.client.Minecraft", true, false, false, false),
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
    new NamePatternFilter("net.minecraft.client.Minecraft", true, false, false, false),
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

You must implement a `Runnable` class to register your transformers, and define it in `fabric.mod.json`.

Example registration class:

```java
public class InternalMassAsmEntrypoint implements Runnable {
    @Override
    public void run() {
        // Register transformers here
    }
}
```

And in `fabric.mod.json`:

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

The internal MassASM example (`InternalMassAsmEntrypoint`) demonstrates:

- Dynamically picking different target classes depending on environment (dev vs production).
- Injecting a log message into the constructor of `Minecraft` using `AdviceAdapter`.

**Example snippet:**

```java
MassASMTransformer.registerVisitor(
    "massasm-internal-inject-init-stdout",
    new NamePatternFilter(
        FabricLauncherBase.getLauncher().isDevelopment()
            ? "net.minecraft.client.Minecraft"
            : "net.minecraft.client.main.Main$2",
        true, false, false, false
    ),
    (className, nextVisitor) -> new CreateTitlePrintTransformer(Opcodes.ASM9, nextVisitor, className)
);
```

This injects the following on constructor entry:
```
[MassASM] net.minecraft.client.Minecraft <init> called, this means our transformer is working, shenanigans now probably ensue. If something fails, look at the weird mod using this API!
```

---

## About `NamePatternFilter`

The `NamePatternFilter` is used to match class names for transformations.

Constructor:

```java
public NamePatternFilter(String pattern, boolean exactContentMatch, boolean startsWith, boolean endsWith, boolean contains)
```

| Parameter | Meaning |
|:---|:---|
| `pattern` | The string pattern to match against the class name. |
| `exactContentMatch` | If `true`, the class name must match exactly (`className.equals(pattern)`). |
| `startsWith` | If `true`, the class name must start with the pattern (`className.startsWith(pattern)`). |
| `endsWith` | If `true`, the class name must end with the pattern (`className.endsWith(pattern)`). |
| `contains` | If `true`, the class name must simply contain the pattern (`className.contains(pattern)`). |

The priority order for matching is:
1. Exact match (if `exactContentMatch` is `true`).
2. `startsWith`, `endsWith`, `contains` — in this order.
3. If none are true, fallback to exact match.

---

## Error Handling

- If MassASM fails to inject due to Fabric internal changes, a stack trace will be printed.
- If any transformer throws during bytecode transformation, an `UnrecoverableMassASMRuntimeError` is thrown.
- **MassASM does not attempt recovery** — fatal transformer errors crash the game.

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for full license text.

---
