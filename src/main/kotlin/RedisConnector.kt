import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

class RedisConnector {
    private val redisHost = System.getenv("REDIS_HOST")
    private val redisPort = System.getenv("REDIS_PORT").toInt()
    private val redisUsername = System.getenv("REDIS_USERNAME")
    private val redisPassword = System.getenv("REDIS_PASSWORD")

    private val pool: JedisPool

    init {
        val poolConfig = JedisPoolConfig()
        pool = JedisPool(poolConfig, redisHost, redisPort, 2000, redisPassword)
    }

    fun connect(): Jedis {
        return pool.resource.apply {
            clientSetname(redisUsername)
        }
    }
}