package com.megamidnight.k8sencoder

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

class RabbitMQConnector {
    fun connect(): Connection {
        val factory = ConnectionFactory().apply {
            host = System.getenv("RABBITMQ_HOST") ?: System.getProperty("RABBITMQ_HOST", "localhost")
            port = (System.getenv("RABBITMQ_PORT") ?: System.getProperty("RABBITMQ_PORT", "5672")).toInt()
            username = System.getenv("RABBITMQ_USERNAME") ?: System.getProperty("RABBITMQ_USERNAME", "guest")
            password = System.getenv("RABBITMQ_PASSWORD") ?: System.getProperty("RABBITMQ_PASSWORD", "guest")
        }
        return factory.newConnection()
    }
}