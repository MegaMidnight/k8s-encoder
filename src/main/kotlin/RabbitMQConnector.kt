import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

class RabbitMQConnector {
    fun connect(): Connection {
        val factory = ConnectionFactory()
        factory.host = System.getenv("RABBITMQ_HOST")
        factory.port = System.getenv("RABBITMQ_PORT").toInt()
        factory.username = System.getenv("RABBITMQ_USERNAME")
        factory.password = System.getenv("RABBITMQ_PASSWORD")

        return factory.newConnection()
    }
}