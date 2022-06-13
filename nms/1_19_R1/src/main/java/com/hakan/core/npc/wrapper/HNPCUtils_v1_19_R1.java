package com.hakan.core.npc.wrapper;

import com.hakan.core.HCore;
import com.hakan.core.npc.HNPC;
import com.hakan.core.npc.skin.HNPCSkin;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

/**
 * HNPCUtils_v1_8_R3 class.
 */
public final class HNPCUtils_v1_19_R1 {

    /**
     * Creates a GameProfile for the NPC.
     *
     * @param skin The skin of the NPC.
     * @return The GameProfile.
     */
    @Nonnull
    public GameProfile createGameProfile(@Nonnull HNPCSkin skin) {
        Objects.requireNonNull(skin, "skin cannot be null!");

        GameProfile profile = new GameProfile(UUID.randomUUID(), UUID.randomUUID().toString().substring(0, 5));
        profile.getProperties().put("textures", new Property("textures", skin.getTexture(), skin.getSignature()));
        return profile;
    }

    /**
     * Creates a new NPC entity.
     *
     * @param skin     The name of the NPC.
     * @param location The location of the NPC.
     * @return The NPC entity.
     */
    @Nonnull
    public EntityPlayer createNPC(@Nonnull HNPCSkin skin, @Nonnull Location location) {
        Objects.requireNonNull(skin, "skin cannot be null!");
        Objects.requireNonNull(location, "location cannot be null!");

        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        WorldServer world = ((CraftWorld) location.getWorld()).getHandle();
        GameProfile profile = this.createGameProfile(skin);

        EntityPlayer entityPlayer = new EntityPlayer(server, world, profile, null);
        entityPlayer.a(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        entityPlayer.persistentInvisibility = false; //set invisibility to true
        entityPlayer.b(5, true); //set invisibility to true
        entityPlayer.c(77.21f); //sets health to 77.21f

        return entityPlayer;
    }

    /**
     * Creates an armor stand to
     * hide name of NPC.
     *
     * @param location The location of armor stand.
     * @return Armor stand.
     */
    @Nonnull
    public EntityArmorStand createNameHider(@Nonnull Location location) {
        Objects.requireNonNull(location, "location cannot be null!");

        WorldServer world = ((CraftWorld) location.getWorld()).getHandle();

        EntityArmorStand armorStand = new EntityArmorStand(world, 0, 0, 0);
        armorStand.persistentInvisibility = true; //set invisibility to true
        armorStand.b(5, true); //set invisibility to true
        armorStand.n(false); //set custom name visibility to true
        armorStand.t(true); //set marker to true
        armorStand.r(false); //set arms to false
        armorStand.s(true); //set no base plate to true
        armorStand.e(true); //set no gravity to true
        armorStand.a(true); //set small to true
        armorStand.c(114.13f); //set health to 114.13 float

        return armorStand;
    }

    /**
     * Creates data watcher for the NPC.
     *
     * @return Data watcher.
     */
    @Nonnull
    public DataWatcher createDataWatcher() {
        DataWatcher dataWatcher = new DataWatcher(null);
        dataWatcher.a(new DataWatcherObject<>(17, DataWatcherRegistry.a), (byte) 127);
        return dataWatcher;
    }

    /**
     * This method walks the NPC to the location
     * that is given. It creates a zombie and
     * villager then make target of zombie to
     * villager and teleport NPC to zombie every tick.
     *
     * @param npc      The NPC.
     * @param to       The location.
     * @param speed    The speed of the NPC.
     * @param callback The callback when the walking over.
     */
    public void walk(@Nonnull HNPC npc, @Nonnull Location to, double speed, @Nonnull Runnable callback) {
        Objects.requireNonNull(npc, "NPC cannot be null!");
        Objects.requireNonNull(to, "location cannot be null!");
        Objects.requireNonNull(callback, "callback cannot be null!");

        Location from = npc.getLocation();
        World toWorld = to.getWorld();
        World fromWorld = from.getWorld();

        if (toWorld == null || !toWorld.equals(fromWorld))
            throw new IllegalArgumentException("cannot walk between different worlds!");

        Villager villager = toWorld.spawn(to, Villager.class);
        villager.setInvisible(true);
        villager.setSilent(true);
        villager.setInvulnerable(true);
        villager.setCustomNameVisible(false);
        villager.setCollidable(false);
        villager.setHealth(11.9123165d);
        villager.setAI(false);

        Zombie zombie = fromWorld.spawn(from, Zombie.class);
        zombie.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        zombie.setInvisible(true);
        zombie.setSilent(true);
        zombie.setInvulnerable(true);
        zombie.setCustomNameVisible(false);
        zombie.setCollidable(false);
        zombie.setHealth(11.9123165d);
        zombie.setAI(true);
        zombie.setTarget(villager);

        AttributeInstance attribute1 = zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        AttributeInstance attribute2 = zombie.getAttribute(Attribute.GENERIC_FOLLOW_RANGE);
        if (attribute1 != null) attribute1.setBaseValue(speed);
        if (attribute2 != null) attribute2.setBaseValue(200);

        HCore.sendPacket(new ArrayList<>(Bukkit.getOnlinePlayers()),
                new PacketPlayOutEntityDestroy(zombie.getEntityId(), villager.getEntityId()));

        HCore.syncScheduler().every(1)
                .run((task) -> {
                    Location zombieLocation = zombie.getLocation();

                    if (zombie.getTarget() == null || !zombie.getTarget().equals(villager))
                        zombie.setTarget(villager);

                    if (zombieLocation.distance(to) < 1 || npc.isDead()) {
                        zombie.remove();
                        villager.remove();
                        npc.setLocation(to);
                        callback.run();
                        task.cancel();
                        return;
                    }

                    npc.setLocation(zombieLocation);
                });
    }
}