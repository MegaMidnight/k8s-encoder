# K8s Video Encoder

A distributed video encoding system designed to run in Kubernetes, utilizing FFmpeg and x265 for efficient video transcoding. The system splits videos into chunks for parallel processing, coordinated through RabbitMQ and Redis.

## Features

- Distributed video encoding using FFmpeg and x265
- Parallel processing of video chunks
- Message queue-based task distribution using RabbitMQ
- Redis-based coordination and state management
- AWS S3 integration for storage
- Docker containerization
- Kubernetes-ready deployment

## Prerequisites

- Java 21 or later
- Docker
- Kubernetes cluster (for production deployment)
- RabbitMQ server
- Redis server
- AWS S3 bucket and credentials

## Building

### Local Build

1. Clone the repository
2. Build using Gradle:
   ```bash
   ./gradlew build
   ```

### Docker Build

Build the Docker image:
```bash
docker build -t k8s-encoder .
```

## Configuration

The application can be configured through environment variables:

- RabbitMQ configuration
- Redis connection details
- AWS S3 credentials
- Encoding parameters

Refer to `Config.kt` for all available configuration options.

## Running

### Local Development

```bash
./gradlew runApp
```

### Docker

```bash
docker run -d \
  -e RABBITMQ_HOST=your-rabbitmq-host \
  -e REDIS_HOST=your-redis-host \
  -e AWS_ACCESS_KEY=your-access-key \
  -e AWS_SECRET_KEY=your-secret-key \
  k8s-encoder
```

## Architecture

The system works by:
1. Splitting input videos into chunks
2. Distributing chunks to worker nodes via RabbitMQ
3. Processing chunks in parallel using FFmpeg and x265
4. Reassembling processed chunks into the final video

## Dependencies

- RabbitMQ AMQP client 5.20.0
- AWS SDK 2.17.52
- Kotlin Coroutines 1.8.0
- Log4j 2.22.1
- Jedis (Redis client) 5.1.0
- FFmpeg (installed in container)
- x265 encoder
- MKVToolNix

## Build System

The project uses Gradle with Kotlin DSL for build configuration. Key tasks:
- `build`: Builds the project
- `jar`: Creates an executable JAR
- `copyClasses`: Copies compiled classes
- `runApp`: Runs the application locally

## Docker Container

The Docker container includes:
- Eclipse Temurin Java 21
- FFmpeg (latest git static build)
- x265 encoder
- MKVToolNix
- Application JAR

## License

License information not specified. Please contact the project maintainers for licensing details.
