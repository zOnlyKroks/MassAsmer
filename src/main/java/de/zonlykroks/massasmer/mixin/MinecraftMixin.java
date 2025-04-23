package de.zonlykroks.massasmer.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
@Debug(export = true)
public class MinecraftMixin {
}
