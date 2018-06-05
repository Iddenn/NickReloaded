/*
 * MIT License
 *
 * Copyright (c) 2017 Antoine "Idden" ROCHAS
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

package io.idden.nickreloaded.nms.v1_11_R1;

import com.mojang.authlib.GameProfile;
import io.idden.nickreloaded.NickReloaded;
import io.idden.nickreloaded.nms.event.PlayerProfileEditorListener;
import io.idden.nickreloaded.utils.ReflectionUtil;
import net.minecraft.server.v1_11_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class v1_11_R1_PlayerIdentityManager
        implements PlayerIdentityManager
{
    private static final Map<UUID, GameProfile> fakeProfiles = new HashMap<>();
    private static Field playerGameProfile, gameProfileId, gameProfileName;
    private static Field playerInfo_action, playerInfo_data;
    private static Field playerInfoData_latency, playerInfoData_gameMode, playerInfoData_gameProfile, playerInfoData_displayName;

    public v1_11_R1_PlayerIdentityManager()
    {
        Map<String, Field> fields = ReflectionUtil.registerFields(PacketPlayOutPlayerInfo.class);
        playerInfo_action = fields.get("a");
        playerInfo_data = fields.get("b");

        Field profileField = null;
        Field profileIdField = null;
        Field profileNameField = null;
        Field pidLatency = null;
        Field pidGameMode = null;
        Field pidGameProfile = null;
        Field pidDisplayName = null;

        try
        {
            profileField = EntityHuman.class.getDeclaredField("bS");
            profileField.setAccessible(true);
            profileIdField = GameProfile.class.getDeclaredField("id");
            profileIdField.setAccessible(true);
            profileNameField = GameProfile.class.getDeclaredField("name");
            profileNameField.setAccessible(true);
            pidLatency = PacketPlayOutPlayerInfo.PlayerInfoData.class.getDeclaredField("b");
            pidLatency.setAccessible(true);
            pidGameMode = PacketPlayOutPlayerInfo.PlayerInfoData.class.getDeclaredField("c");
            pidGameMode.setAccessible(true);
            pidGameProfile = PacketPlayOutPlayerInfo.PlayerInfoData.class.getDeclaredField("d");
            pidGameProfile.setAccessible(true);
            pidDisplayName = PacketPlayOutPlayerInfo.PlayerInfoData.class.getDeclaredField("e");
            pidDisplayName.setAccessible(true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        playerGameProfile = profileField;
        gameProfileId = profileIdField;
        gameProfileName = profileNameField;
        playerInfoData_latency = pidLatency;
        playerInfoData_gameMode = pidGameMode;
        playerInfoData_gameProfile = pidGameProfile;
        playerInfoData_displayName = pidDisplayName;

        NickReloaded.INSTANCE.getServer().getPluginManager().registerEvents(new PlayerProfileEditorListener(fakeProfiles, this), NickReloaded.INSTANCE);
    }

    @Override
    public void updatePlayerProfile(Object packet)
    {
        try
        {
            PacketPlayOutPlayerInfo.EnumPlayerInfoAction action = (PacketPlayOutPlayerInfo.EnumPlayerInfoAction) playerInfo_action.get(packet);
            if (action != PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER)
            {
                return;
            }
            List<PacketPlayOutPlayerInfo.PlayerInfoData> dataList = (List<PacketPlayOutPlayerInfo.PlayerInfoData>) playerInfo_data.get(packet);
            for (PacketPlayOutPlayerInfo.PlayerInfoData data : dataList)
            {
                GameProfile gameProfile = data.a();
                if (fakeProfiles.containsKey(gameProfile.getId()))
                {
                    playerInfoData_gameProfile.set(data,
                                                   fakeProfiles.get(gameProfile.getId()));
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
        setProfileName(gameProfile,
                       name);
        updatePlayer(player,
                     false);
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
        /*GameProfile skinProfile = NickReloaded.get().getPayloadManager().getGameprofileFiller().fillGameprofile(new GameProfile(null,
        skin));


        for (Property texture : skinProfile.getProperties().get("textures"))
        {
            gameProfile.getProperties().put("textures",
                                            texture);
        }*/
        updatePlayer(player,
                     true);
    }

    @Override
    public void updatePlayer(Player player, boolean isSkinChanging)
    {
        final EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        final UUID uuid = player.getUniqueId();
        PacketPlayOutEntityDestroy destroyPacket = new PacketPlayOutEntityDestroy(entityPlayer.getId());
        for (Player p : Bukkit.getServer().getOnlinePlayers())
        {
            if (! p.getUniqueId().equals(uuid))
            {
                ReflectionUtil.sendPacket(p,
                                      destroyPacket);
            }
        }
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                PacketPlayOutPlayerInfo playerInfo = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER,
                                                                                 entityPlayer);
                PacketPlayOutNamedEntitySpawn spawnPacket = new PacketPlayOutNamedEntitySpawn(entityPlayer);
                for (Player player : Bukkit.getServer().getOnlinePlayers())
                {
                    ReflectionUtil.sendPacket(player,
                                          playerInfo);
                    if (! player.getUniqueId().equals(uuid))
                    {
                        ReflectionUtil.sendPacket(player,
                                              spawnPacket);
                    }
                    else
                    {
                        if (isSkinChanging)
                        {
                            boolean isFlying = player.isFlying();
                            ReflectionUtil.sendPacket(player,
                                                  new PacketPlayOutRespawn(player.getWorld().getEnvironment().getId(),
                                                                            entityPlayer.getWorld().getDifficulty(),
                                                                            entityPlayer.getWorld().worldData.getType(),
                                                                            entityPlayer.playerInteractManager.getGameMode()));
                            player.teleport(player.getLocation(),
                                            PlayerTeleportEvent.TeleportCause.PLUGIN);
                            player.setFlying(isFlying);
                        }
                        player.updateInventory();
                    }
                }

                updatePlayerProfile(playerInfo);
            }
        }.runTaskLater(NickReloaded.INSTANCE,
                       0);
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
            GameProfile fakeProfile = new GameProfile(player.getUniqueId(),
                                                      player.getName());
            fakeProfile.getProperties().replaceValues("textures",
                                                      getPlayerProfile(player).getProperties().get("textures"));
            fakeProfiles.put(uuid,
                             fakeProfile);
            return fakeProfile;
        }
    }

    @Override
    public GameProfile getPlayerProfile(Player player)
    {
        try
        {
            return (GameProfile) playerGameProfile.get(((CraftPlayer) player).getHandle());
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
            gameProfileName.set(gameProfile,
                                name);
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
            gameProfileId.set(gameProfile,
                              uuid);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
