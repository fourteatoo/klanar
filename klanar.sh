#!/bin/sh

jar=$0.jar

JAVA_VERSION=21+
export JAVA_VERSION

exec java -jar $jar "$@"
