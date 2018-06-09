/*
 *  MIT License
 *
 *  Copyright (c) 2017-2018 Antoine "Idden" ROCHAS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.idden.nickreloaded.version.impl;

import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.idden.nickreloaded.NickReloaded;
import io.idden.nickreloaded.listener.PlayerProfileEditorListener;
import io.idden.nickreloaded.utils.ReflectionUtil;
import io.idden.nickreloaded.version.wrapper.VersionWrapper;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Wrapper1_12_R1 implements VersionWrapper
{
    private static final Map<UUID, GameProfile> fakeProfiles = new HashMap<>();

    private Field playerGP, gpID, gpName;
    private Field piAction, piData;
    private Field pidLatency, pidGamemode, pidGameprofile, pidDisplayName;

    public Wrapper1_12_R1()
    {
        Map<String, Field> fields = ReflectionUtil.registerFields(PacketPlayOutPlayerInfo.class);
        piAction = fields.get("a");
        piData = fields.get("b");

        try
        {
            playerGP = EntityHuman.class.getDeclaredField("g");
            playerGP.setAccessible(true);
            gpID = GameProfile.class.getDeclaredField("id");
            gpID.setAccessible(true);
            gpName = GameProfile.class.getDeclaredField("name");
            gpName.setAccessible(true);
            pidLatency = PacketPlayOutPlayerInfo.PlayerInfoData.class.getDeclaredField("b");
            pidLatency.setAccessible(true);
            pidGamemode = PacketPlayOutPlayerInfo.PlayerInfoData.class.getDeclaredField("c");
            pidGamemode.setAccessible(true);
            pidGameprofile = PacketPlayOutPlayerInfo.PlayerInfoData.class.getDeclaredField("d");
            pidGameprofile.setAccessible(true);
            pidDisplayName = PacketPlayOutPlayerInfo.PlayerInfoData.class.getDeclaredField("e");
            pidDisplayName.setAccessible(true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        NickReloaded.INSTANCE.getServer().getPluginManager().registerEvents(new PlayerProfileEditorListener(fakeProfiles, this), NickReloaded.INSTANCE);
    }

    @Override
    public void sendActionbar(Player player, String message)
    {
        IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + message + "\"}");

        PacketPlayOutChat bar = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO);

        ReflectionUtil.sendPacket(player, bar);
    }

    @Override
    public GameProfile fillGameprofile(GameProfile gameProfile)
    {
        try
        {
            if (gameProfile != null)
            {
                GameProfile gameProfile1 = null;
                if (gameProfile.getName() != null)
                {
                    gameProfile1 = MinecraftServer.getServer().getUserCache().getProfile(gameProfile.getName());
                }
                if (gameProfile1 == null)
                {
                    gameProfile1 = MinecraftServer.getServer().getUserCache().a(gameProfile.getId());
                }
                if (gameProfile1 == null)
                {
                    gameProfile1 = gameProfile;
                }
                if (Iterables.getFirst(gameProfile1.getProperties().get("textures"), null) == null)
                {
                    gameProfile1 = MinecraftServer.getServer().az().fillProfileProperties(gameProfile1, true);
                }
                return gameProfile1;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void updatePlayerProfile(Object packet)
    {
        try
        {
            PacketPlayOutPlayerInfo.EnumPlayerInfoAction action = (PacketPlayOutPlayerInfo.EnumPlayerInfoAction) piAction.get(packet);
            if (action != PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER)
            {
                return;
            }
            List<PacketPlayOutPlayerInfo.PlayerInfoData> dataList = (List<PacketPlayOutPlayerInfo.PlayerInfoData>) piData.get(packet);
            for (PacketPlayOutPlayerInfo.PlayerInfoData data : dataList)
            {
                GameProfile gameProfile = data.a();
                if (fakeProfiles.containsKey(gameProfile.getId()))
                {
                    pidGameprofile.set(data, fakeProfiles.get(gameProfile.getId()));
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void setPlayerName(Player player, String name)
    {
        GameProfile gameProfile = getFakeProfile(player);
        setProfileName(gameProfile, name);
        updatePlayer(player, false);
    }

    @Override
    public String getPlayerName(Player player)
    {
        return getFakeProfile(player).getName();
    }

    @Override
    public void setPlayerSkin(Player player, String skin)
    {
        GameProfile gameProfile = getFakeProfile(player);
        gameProfile.getProperties().get("textures").clear();
        GameProfile skinProfile = fillGameprofile(new GameProfile(null, skin));


        for (Property texture : skinProfile.getProperties().get("textures"))
        {
            gameProfile.getProperties().put("textures", texture);
        }

        updatePlayer(player, true);
    }

    @Override
    public void updatePlayer(Player player, boolean isSkinChanging)
    {
        final EntityPlayer         entityPlayer  = ((CraftPlayer) player).getHandle();
        final UUID                 uuid          = player.getUniqueId();
        PacketPlayOutEntityDestroy destroyPacket = new PacketPlayOutEntityDestroy(entityPlayer.getId());
        for (Player p : Bukkit.getServer().getOnlinePlayers())
        {
            if (! p.getUniqueId().equals(uuid))
            {
                ReflectionUtil.sendPacket(p, destroyPacket);
            }
        }
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                PacketPlayOutPlayerInfo       playerInfo  = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER, entityPlayer);
                PacketPlayOutNamedEntitySpawn spawnPacket = new PacketPlayOutNamedEntitySpawn(entityPlayer);
                for (Player player : Bukkit.getServer().getOnlinePlayers())
                {
                    ReflectionUtil.sendPacket(player, playerInfo);
                    if (! player.getUniqueId().equals(uuid))
                    {
                        ReflectionUtil.sendPacket(player, spawnPacket);
                    }
                    else
                    {
                        if (isSkinChanging)
                        {
                            boolean isFlying = player.isFlying();
                            ReflectionUtil.sendPacket(player, new PacketPlayOutRespawn(player.getWorld().getEnvironment().getId(), entityPlayer.getWorld().getDifficulty(), entityPlayer.getWorld().worldData.getType(), entityPlayer.playerInteractManager.getGameMode()));
                            player.teleport(player.getLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
                            player.setFlying(isFlying);
                        }
                        player.updateInventory();
                    }
                }

                updatePlayerProfile(playerInfo);
            }
        }.runTaskLater(NickReloaded.INSTANCE, 0);
    }

    @Override
    public GameProfile getFakeProfile(Player player)
    {
        UUID uuid = player.getUniqueId();
        if (fakeProfiles.containsKey(uuid))
        {
            return fakeProfiles.get(uuid);
        }
        else
        {
            GameProfile fakeProfile = new GameProfile(player.getUniqueId(), player.getName());
            fakeProfile.getProperties().replaceValues("textures", getPlayerProfile(player).getProperties().get("textures"));
            fakeProfiles.put(uuid, fakeProfile);
            return fakeProfile;
        }
    }

    @Override
    public GameProfile getPlayerProfile(Player player)
    {
        try
        {
            return (GameProfile) playerGP.get(((CraftPlayer) player).getHandle());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void setProfileName(GameProfile gameProfile, String name)
    {
        try
        {
            gpName.set(gameProfile, name);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void setProfileId(GameProfile gameProfile, UUID uuid)
    {
        try
        {
            gpID.set(gameProfile, uuid);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
