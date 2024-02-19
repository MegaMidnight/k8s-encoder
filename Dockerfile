# Use a base image with Java 21
FROM eclipse-temurin:21-jammy

# Set the working directory in the container
WORKDIR /app

# Copy the JAR file to the container
COPY ./build/libs/kotlin-ffmpeg-x265-1.0.0.jar /app/

# Install wget, xz-utils, x265 and libx265-dev
RUN apt-get update && \
    apt-get install -y wget xz-utils x265 libx265-dev

# Download and extract the pre-compiled FFmpeg build
RUN wget https://johnvansickle.com/ffmpeg/builds/ffmpeg-git-amd64-static.tar.xz && \
    tar xvf ffmpeg-git-amd64-static.tar.xz && \
    rm ffmpeg-git-amd64-static.tar.xz

# Correct the path to the FFmpeg binary
RUN mv ffmpeg-git-*-static/ffmpeg /usr/bin/

# Specify the entry point for running the application
ENTRYPOINT ["java", "-jar", "/app/kotlin-ffmpeg-x265-1.0.0.jar"]