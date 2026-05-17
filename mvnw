#!/bin/sh
# Apache Maven Wrapper startup script, version 3.3.4

WRAPPER_DIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="${WRAPPER_DIR}/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

if [ ! -f "${WRAPPER_JAR}" ]; then
    echo "Downloading Maven Wrapper..."
    mkdir -p "$(dirname "${WRAPPER_JAR}")"
    curl -fsSL -o "${WRAPPER_JAR}" "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.4/maven-wrapper-3.3.4.jar"
fi

exec java -cp "${WRAPPER_JAR}" ${WRAPPER_LAUNCHER} "$@"
