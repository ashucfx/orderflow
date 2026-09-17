#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Start Up Shell script
# ----------------------------------------------------------------------------

if [ -z "$JAVA_HOME" ]; then
    JAVA_CMD="java"
else
    JAVA_CMD="$JAVA_HOME/bin/java"
fi

MAVEN_PROJECTBASEDIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER="org.apache.maven.wrapper.MavenWrapperMain"
DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

if [ ! -e "$WRAPPER_JAR" ]; then
    echo "Downloading Maven wrapper..."
    curl -o "$WRAPPER_JAR" "$DOWNLOAD_URL" || wget -O "$WRAPPER_JAR" "$DOWNLOAD_URL"
fi

exec "$JAVA_CMD" \
    -classpath "$WRAPPER_JAR" \
    "$WRAPPER_LAUNCHER" "$@"
