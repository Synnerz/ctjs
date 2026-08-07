package com.chattriggers.ctjs.internal.mixins.commands;

import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(EntitySelector.class)
public interface EntitySelectorAccessor {
    @Accessor
    int getMaxResults();

    @Accessor
    boolean getIncludesEntities();

    @Accessor
    List<Predicate<Entity>> getContextFreePredicates();

    @Accessor
    MinMaxBounds.Doubles getRange();

    @Accessor
    Function<Vec3, Vec3> getPosition();

    @Accessor
    AABB getAabb();

    @Accessor
    BiConsumer<Vec3, List<? extends Entity>> getOrder();

    @Accessor
    boolean getCurrentEntity();

    @Accessor
    String getPlayerName();

    @Accessor
    UUID getEntityUUID();

    @Accessor
    EntityTypeTest<Entity, ?> getType();
}
