#!/bin/sh
APP_BASE_NAME=`basename "$0"`
DIRNAME=`dirname "$0"`
if [ "$DIRNAME" = "" ]; then
    DIRNAME=.
fi
APP_HOME=`cd "$DIRNAME" && pwd`
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
exec java -jar "$CLASSPATH" "$@"