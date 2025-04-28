**MassASM API**

A lightweight Fabric-based API for registering on-the-fly ASM bytecode transformers across all game classes. MassASM extends Fabric's `GameTransformer` to apply custom transformations *after* the Fabric Minecraft patches but *before* mixins, keep that in mind for compats sake.

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
    - [Custom Registration Entrypoint](#custom-registration-entrypoint)
5. [Examples](#examples)
6. [Error Handling](#error-handling)
7. [License](#license)

---

## Getting Started

1. **Include** the `massasmer` package in your Fabric mod dependencies.
2. **Implement** a PreLaunchEntrypoint to inject the `MassASMTransformer` into Fabric’s game pipeline.
3. **Register** your transformers by providing a unique name, a class-filter predicate, and a transformer implementation.

---

## Key Components

| Class                       | Description |
|-----------------------------|-------------|
| `MassASMTransformer`        | Extends Fabric's `GameTransformer`; manages a list of registered transformers and applies them globally. |
| `InternalMassAsmEntrypoint` | Example `Runnable` entrypoint that registers a simple visitor-based transformer. |
| `MassasmerPreLaunch`        | Implements Fabric’s `PreLaunchEntrypoint`; replaces the game provider's transformer with `MassASMTransformer`. |

---

## Registering Transformers

MassASM provides three registration methods, each accepting a **name**, a **filter predicate**, and a **transformer implementation**.

### Raw Bytecode Transformers

```java
MassASMTransformer.register(
    "unique-name",
    className -> className.startsWith("com.example"),
    (className, classBytes) -> {
        // return a new VisitorProvider
    }
);
```

### ClassNode Transformers

```java
MassASMTransformer.registerNodeTransformer(
    "node-transformer",
    className -> className.equals("net.minecraft.client.Minecraft"),
    (className, classNode) -> {
        // return a new ClassNodeTransformer
    }
);
```

### Visitor-Based Transformers

```java
MassASMTransformer.registerVisitor(
    "visitor-transformer",
    className -> className.equals("net.minecraft.client.Minecraft"),
    (className, writer) -> new ClassVisitor(Opcodes.ASM9, writer) {
        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            if ("<init>".equals(name)) {
                return new AdviceAdapter(api, mv, access, name, descriptor) {
                    @Override
                    protected void onMethodEnter() {
                        // injection code here
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

### Custom Registration Entrypoint

Any class listed under the `mass-asm` entrypoint in your `fabric.mod.json` that implements `Runnable` will be executed during pre-launch. Use this to register your transformers:

```java
public class InternalMassAsmEntrypoint implements Runnable {
    @Override
    public void run() {
        // register your transformers here
    }
}
```

Include in `fabric.mod.json`:

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

See `InternalMassAsmEntrypoint` for a simple visitor that logs when `net.minecraft.client.Minecraft` is constructed.

The visitor implementation prints a startup message to `System.out` inside the Minecraft constructor.

---

## Error Handling

- If reflection on the game provider fails, a stack trace will be printed.
- During transformation, any unchecked exception will be wrapped in `UnrecoverableMassASMRuntimeError`.
- If we fail, we fail HARD. We will not attempt to recover or continue the game.
---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

