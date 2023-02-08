#!/bin/sh
gradle assemble --stacktrace
test "$MAVEN_USER" -a "$MAVEN_PASS" -a \
  -z "$BUILD_BASE_REF" -a -z "$BUILD_HEAD_REF" &&
  gradle publish
