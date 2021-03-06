package fr.antoinerochas.nickreloaded.core.storage;

import fr.antoinerochas.nickreloaded.core.logger.NRLogger;
import fr.antoinerochas.nickreloaded.core.manager.StorageManager;
import fr.antoinerochas.nickreloaded.core.storage.core.CacheStorageMode;
import fr.antoinerochas.nickreloaded.core.storage.mysql.Field;
import fr.antoinerochas.nickreloaded.core.storage.mysql.Table;
import fr.antoinerochas.nickreloaded.core.storage.redisson.RedisManager;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.HashMap;
import java.util.UUID;

public class AccountProvider
{
    private UUID uuid;
    private Field fuuid;

    private RedisManager redissonAccess;

    private final String ACCOUNT_KEY = "nickdata:";

    //USED ONLY IF REDIS IS NOT USED !
    private HashMap<UUID, NRData> DATA = new HashMap<>();

    public AccountProvider(UUID uuid)
    {
        this.uuid = uuid;
        this.fuuid = new Field("UUID",
                               uuid);
        this.redissonAccess = RedisManager.INSTANCE;
    }

    public NRData getData()
    {
        NRData nickData = getDataFromCache();

        if (nickData == null)
        {
            nickData = getDataFromStorage();
        }

        return nickData;
    }

    private NRData getDataFromStorage()
    {
        Table main = StorageManager.getInstance().getMainTable();

        boolean account = main.select(fuuid.getName(),
                                      fuuid) != null;

        if(! account)
        {
            //CREATE ACCOUNT
            HashMap<String, Object> FIELDS = new HashMap<>();

            FIELDS.put("UUID", fuuid.getValue());
            FIELDS.put("NICKED", 0);
            FIELDS.put("NAME", null);
            FIELDS.put("SKIN", null);

            main.insert(FIELDS);

            NRLogger.log(NRLogger.NRLPrefix.INFO, "Created new account for UUID: '" + uuid + "'.");
        }

        boolean nicked = (boolean) main.select("NICKED", fuuid);
        String name = (String) main.select("NAME", fuuid);
        String skin = (String) main.select("SKIN", fuuid);

        return new NRData(uuid,
                          nicked,
                          name,
                          skin);
    }

    private NRData getDataFromCache()
    {
        if(CacheStorageMode.isMode(CacheStorageMode.REDIS))
        {
            final RedissonClient redissonClient = redissonAccess.getRedissonClient();
            final String key = ACCOUNT_KEY + uuid.toString();
            final RBucket<NRData> accountBucket = redissonClient.getBucket(key);

            sendDataToCache(accountBucket.get());

            return accountBucket.get();
        }
        else
        {
            throw new UnsupportedOperationException("Not available");
        }
    }

    public void sendDataToCache(NRData account)
    {
        if(CacheStorageMode.isMode(CacheStorageMode.REDIS))
        {
            final RedissonClient redissonClient = redissonAccess.getRedissonClient();
            final String key = ACCOUNT_KEY + uuid.toString();
            final RBucket<NRData> accountBucket = redissonClient.getBucket(key);

            accountBucket.set(account);
        }
        else
        {
            throw new UnsupportedOperationException("Not available");
        }
    }

    public void sendDataToStorage(NRData account)
    {
        final RedissonClient redissonClient = redissonAccess.getRedissonClient();
        final String key = ACCOUNT_KEY + uuid.toString();
        final RBucket<NRData> accountBucket = redissonClient.getBucket(key);

        accountBucket.set(account);
    }
}