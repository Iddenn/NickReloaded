package fr.antoinerochas.nickreloaded.api.storage;

import fr.antoinerochas.nickreloaded.api.config.ConfigFileValues;
import fr.antoinerochas.nickreloaded.api.logger.NickReloadedLogger;
import fr.antoinerochas.nickreloaded.api.manager.StorageManager;
import fr.antoinerochas.nickreloaded.api.storage.mysql.Field;
import fr.antoinerochas.nickreloaded.api.storage.mysql.Table;
import fr.antoinerochas.nickreloaded.api.storage.redisson.RedisManager;
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
    private HashMap<UUID, NickData> DATA = new HashMap<>();

    public AccountProvider(UUID uuid)
    {
        this.uuid = uuid;
        this.fuuid = new Field("UUID",
                               uuid);
        this.redissonAccess = RedisManager.INSTANCE;
    }

    public NickData getAccount()
    {
        NickData nickData = getDataFromCache();

        if (nickData == null)
        {
            nickData = getDataFromStorage();
        }

        return nickData;
    }

    public NickData getDataFromStorage()
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

            NickReloadedLogger.log(NickReloadedLogger.Level.INFO, "Created new account for UUID: '" + uuid + "'.");
        }

        boolean nicked = (boolean) main.select("NICKED", fuuid);
        String name = (String) main.select("NAME", fuuid);
        String skin = (String) main.select("SKIN", fuuid);

        return new NickData(uuid,
                            nicked,
                            name,
                            skin);
    }

    public NickData getDataFromCache()
    {
        if(StorageManager.getInstance().getConfigFile().getFileConfiguration().getBoolean(ConfigFileValues.STORAGE_COMMON_REDIS.getValue()))
        {
            final RedissonClient redissonClient = redissonAccess.getRedissonClient();
            final String key = ACCOUNT_KEY + uuid.toString();
            final RBucket<NickData> accountBucket = redissonClient.getBucket(key);

            sendAccountDataToRedis(accountBucket.get());

            return accountBucket.get();
        }
        else
        {
        }

        return null;
    }

    public void sendAccountDataToRedis(NickData account)
    {
        final RedissonClient redissonClient = redissonAccess.getRedissonClient();
        final String key = ACCOUNT_KEY + uuid.toString();
        final RBucket<NickData> accountBucket = redissonClient.getBucket(key);

        accountBucket.set(account);
    }
}
