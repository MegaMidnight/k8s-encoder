# Build stage. Dev variant includes apk and shell. Never shipped.
FROM dhi.io/eclipse-temurin:25-alpine3.23-dev AS builder

# FFmpeg is packaged with x265 support. Alpine patches these so no external downloads are needed.
RUN apk add --no-cache \
    ffmpeg \
    mkvtoolnix

# Copy binaries and their shared library dependencies into /collect.
# ldd resolves what each binary needs. The file check skips virtual entries like linux-vdso.
RUN mkdir -p /collect/bin /collect/lib && \
    for bin in /usr/bin/ffmpeg /usr/bin/mkvmerge /usr/bin/mkvextract /usr/bin/mkvinfo /usr/bin/mkvpropedit; do \
        cp "$bin" /collect/bin/; \
        ldd "$bin" | awk '/=>/ { print $3 }' | xargs -I{} sh -c 'test -f "{}" && cp "{}" /collect/lib/'; \
    done


# Runtime stage. No shell, no package manager. Keep this Alpine version in sync with the builder
# so the copied shared libraries are compatible with the runtime libc.
FROM dhi.io/eclipse-temurin:25-alpine3.23

COPY --from=builder /collect/bin/ /usr/local/bin/
COPY --from=builder /collect/lib/ /usr/local/lib/
COPY ./build/libs/kotlin-ffmpeg-x265-1.0.0.jar /app/kotlin-ffmpeg-x265-1.0.0.jar

WORKDIR /app

ENTRYPOINT ["java", "-jar", "/app/kotlin-ffmpeg-x265-1.0.0.jar"]
