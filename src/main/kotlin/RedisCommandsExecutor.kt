import redis.clients.jedis.exceptions.JedisConnectionException

class RedisCommandsExecutor {
    fun <T> retryRedisCommand(command: () -> T): T {
        val maxAttempts = 4
        (1..maxAttempts).forEach { attempt ->
            try {
                return command() // Execute the Redis command
            } catch (e: JedisConnectionException) {
                if (attempt == maxAttempts) throw e
                println("Failed to execute Redis operation. Attempt $attempt/$maxAttempts failed. Retrying...")
                Thread.sleep(1500) // Wait for 1.5 seconds before retrying
            } catch (e: Exception) {
                println("An error occurred during the Redis operation: ${e.message}")
            }
        }
        throw RuntimeException("Failed to execute Redis operation after $maxAttempts attempts")
    }
}