#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup script for Unix
#
# Optional ENV vars
# -----------------
#   JAVA_HOME - location of a JDK home dir, otherwise 'java' will be used
#   MAVEN_OPTS - parameters passed to the Java VM when running Maven
#                e.g. to debug Maven itself, use
#                set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
#   MAVEN_SKIP_RC - flag to disable loading of mavenrc files
# ----------------------------------------------------------------------------

if [ -z "$MAVEN_SKIP_RC" ]; then
  if [ -f /etc/mavenrc ]; then
    . /etc/mavenrc
  fi
  if [ -f "$HOME/.mavenrc" ]; then
    . "$HOME/.mavenrc"
  fi
fi

# OS specific support. $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
mingw=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true ;;
  MINGW*) mingw=true ;;
esac

if [ -z "$JAVA_HOME" ]; then
  java_exe=`which java 2> /dev/null`
  if [ -z "$java_exe" ]; then
    if [ -n "$JAVA_HOME" ]; then
      java_exe="$JAVA_HOME/bin/java"
    else
      echo "Error: JAVA_HOME is not defined and 'java' could not be found in your PATH." >&2
      exit 1
    fi
  fi
else
  java_exe="$JAVA_HOME/bin/java"
fi

if [ ! -x "$java_exe" ]; then
  echo "Error: JAVA_HOME is not defined correctly." >&2
  echo "  We cannot execute $java_exe" >&2
  exit 1
fi

wrapper_jar=".mvn/wrapper/maven-wrapper.jar"
if [ ! -f "$wrapper_jar" ]; then
  echo "Error: $wrapper_jar not found. Please run 'mvn wrapper:wrapper' to download it." >&2
  exit 1
fi

exec "$java_exe" \
  $MAVEN_OPTS \
  -classpath "$wrapper_jar" \
  "-Dmaven.home=$M2_HOME" \
  "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
  org.apache.maven.wrapper.MavenWrapperMain \
  "$@"
