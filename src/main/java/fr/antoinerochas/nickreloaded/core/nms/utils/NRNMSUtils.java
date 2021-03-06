package fr.antoinerochas.nickreloaded.core.nms.utils;

import fr.antoinerochas.nickreloaded.core.manager.NRWrapperLoader;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class NRNMSUtils
{

    private final static Map<Class<?>, Map<String, Field>> classFields = new HashMap<Class<?>, Map<String, Field>>();

    public static void sendPacket(Player player, Object packet)
    {
        try
        {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection, packet);
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static Class<?> getNMSClass(String name)
    {
        try
        {
            return Class.forName("net.minecraft.server." + new NRWrapperLoader().getBukkitVersion() + "." + name);
        }

        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static Map<String, Field> registerFields(Class<?> cl)
    {
        if (classFields.containsKey(cl))
        {
            return classFields.get(cl);
        }
        Map<String, Field> fields = new HashMap<>();
        for (Field field : cl.getDeclaredFields())
        {
            field.setAccessible(true);
            fields.put(field.getName(),
                       field);
        }
        classFields.put(cl,
                        fields);
        return fields;
    }

    public static Map<String, Field> getFields(Class<?> cl)
    {
        if (classFields.containsKey(cl))
        {
            return classFields.get(cl);
        }
        return null;
    }
}
