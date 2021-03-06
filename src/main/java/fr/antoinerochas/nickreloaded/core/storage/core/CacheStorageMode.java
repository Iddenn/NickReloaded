package fr.antoinerochas.nickreloaded.core.storage.core;

public enum CacheStorageMode
{
    REDIS,
    SOCKETS;

    private static CacheStorageMode databaseStorageMode;

    public static CacheStorageMode getMode()
    {
        return databaseStorageMode;
    }


    public static boolean isMode(CacheStorageMode mode)
    {
        return databaseStorageMode == mode;
    }

    public static void setMode(CacheStorageMode mode)
    {
        databaseStorageMode = mode;
    }
}
