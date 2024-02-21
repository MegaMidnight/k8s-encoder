import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.net.URI

class RedisConnector {
    private val redisConnectionString = System.getenv("REDIS_CONNECTION_STRING")
    private val pool: JedisPool

    init {
        val poolConfig = JedisPoolConfig()
        pool = JedisPool(poolConfig, URI.create(redisConnectionString))
    }

    fun connect(): Jedis {
        return pool.resource
    }
}