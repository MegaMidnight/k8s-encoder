package com.megamidnight.k8sencoder

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import com.megamidnight.k8sencoder.util.MockClearingExtension

@ExtendWith(MockClearingExtension::class)
@DisplayName("RabbitMQConnector")
class RabbitMQConnectorTest {
    private lateinit var mockConnectionFactory: ConnectionFactory
    private lateinit var mockConnection: Connection
    private lateinit var connector: RabbitMQConnector

    @BeforeEach
    fun setUp() {
        // Store original environment variables
        System.setProperty("RABBITMQ_HOST", "localhost")
        System.setProperty("RABBITMQ_PORT", "5672")
        System.setProperty("RABBITMQ_USERNAME", "guest")
        System.setProperty("RABBITMQ_PASSWORD", "guest")

        // Setup mocks
        mockConnectionFactory = mockk(relaxed = true)
        mockConnection = mockk(relaxed = true)
        every { mockConnectionFactory.newConnection() } returns mockConnection

        // Mock ConnectionFactory creation
        mockkConstructor(ConnectionFactory::class)
        every { anyConstructed<ConnectionFactory>().host = any() } just Runs
        every { anyConstructed<ConnectionFactory>().port = any() } just Runs
        every { anyConstructed<ConnectionFactory>().username = any() } just Runs
        every { anyConstructed<ConnectionFactory>().password = any() } just Runs
        every { anyConstructed<ConnectionFactory>().newConnection() } returns mockConnection

        connector = RabbitMQConnector()
    }

    @AfterEach
    fun tearDown() {
        System.clearProperty("RABBITMQ_HOST")
        System.clearProperty("RABBITMQ_PORT")
        System.clearProperty("RABBITMQ_USERNAME")
        System.clearProperty("RABBITMQ_PASSWORD")
    }

    @Test
    @DisplayName("should create connection with default configuration")
    fun createConnectionWithDefaultConfig() {
        // when
        val connection = connector.connect()

        // then
        assertThat(connection).isSameAs(mockConnection)
        verify {
            anyConstructed<ConnectionFactory>().apply {
                host = "localhost"
                port = 5672
                username = "guest"
                password = "guest"
                newConnection()
            }
        }
    }

    @Test
    @DisplayName("should create connection with custom configuration")
    fun createConnectionWithCustomConfig() {
        // given
        System.setProperty("RABBITMQ_HOST", "rabbitmq.example.com")
        System.setProperty("RABBITMQ_PORT", "15672")
        System.setProperty("RABBITMQ_USERNAME", "admin")
        System.setProperty("RABBITMQ_PASSWORD", "secret")

        // when
        val connection = connector.connect()

        // then
        assertThat(connection).isSameAs(mockConnection)
        verify {
            anyConstructed<ConnectionFactory>().apply {
                host = "rabbitmq.example.com"
                port = 15672
                username = "admin"
                password = "secret"
                newConnection()
            }
        }
    }

    @Test
    @DisplayName("should handle connection errors")
    fun handleConnectionErrors() {
        // given
        every { anyConstructed<ConnectionFactory>().newConnection() } throws RuntimeException("Connection failed")

        // when/then
        assertThrows<RuntimeException> {
            connector.connect()
        }
    }

    @Test
    @DisplayName("should handle invalid port number")
    fun handleInvalidPortNumber() {
        // given
        System.setProperty("RABBITMQ_PORT", "invalid")

        // when/then
        assertThrows<IllegalArgumentException> {
            connector.connect()
        }
    }
}
