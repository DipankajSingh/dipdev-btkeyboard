#!/bin/sh
# Gradle wrapper script — standard boilerplate
APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")
APP_HOME="$(cd "$(dirname "$0")" && pwd -P)"
MAX_FD=maximum
warn () { echo "$*"; } >&2
die () { echo; echo "$*"; echo; exit 1; } >&2
if [ "$APP_HOME" = "" ]; then APP_HOME=.; fi
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine JVM command
if [ -n "$JAVA_HOME" ]; then
    if [ -x "$JAVA_HOME/jre/sh/java" ]; then JAVACMD=$JAVA_HOME/jre/sh/java
    else JAVACMD=$JAVA_HOME/bin/java; fi
    if [ ! -x "$JAVACMD" ]; then die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"; fi
else
    JAVACMD=java
    if ! command -v java >/dev/null 2>&1; then die "ERROR: JAVA_HOME is not set and java is not in PATH."; fi
fi

exec "$JAVACMD" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
