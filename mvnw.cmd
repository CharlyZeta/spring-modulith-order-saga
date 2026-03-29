@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup script for Windows
@REM
@REM Optional ENV vars
@REM -----------------
@REM   JAVA_HOME - location of a JDK home dir, otherwise 'java' will be used
@REM   MAVEN_OPTS - parameters passed to the Java VM when running Maven
@REM                e.g. to debug Maven itself, use
@REM                set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
@REM   MAVEN_SKIP_RC - flag to disable loading of mavenrc files
@REM ----------------------------------------------------------------------------

@echo off
@setlocal

if not "%MAVEN_SKIP_RC%" == "" goto skipRcPre
@if exist "%PROGRAMDATA%\mavenrc.cmd" call "%PROGRAMDATA%\mavenrc.cmd" %*
@if exist "%USERPROFILE%\mavenrc.cmd" call "%USERPROFILE%\mavenrc.cmd" %*
:skipRcPre

@setlocal

set ERROR_CODE=0

@REM To isolate internal variables from possible wrapper scripts, we use _EL_ prefix
set _EL_JAVA_HOME=%JAVA_HOME%

if not "%_EL_JAVA_HOME%" == "" goto haveJavaHome
set _EL_JAVA_EXE=java.exe
%_EL_JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto haveJava
echo.
echo Error: JAVA_HOME is not defined correctly.
echo   We cannot execute %_EL_JAVA_EXE%
goto error

:haveJavaHome
set _EL_JAVA_EXE="%_EL_JAVA_HOME%\bin\java.exe"
if exist %_EL_JAVA_EXE% goto haveJava
echo.
echo Error: JAVA_HOME is not defined correctly.
echo   We cannot execute %_EL_JAVA_EXE%
goto error

:haveJava
set _EL_WRAPPER_JAR="%~dp0.mvn\wrapper\maven-wrapper.jar"
if exist %_EL_WRAPPER_JAR% goto run
echo.
echo Error: %_EL_WRAPPER_JAR% not found.
goto error

:run
set _EL_MAVEN_HOME=%M2_HOME%
set _EL_PROJECT_DIR=%~dp0

%_EL_JAVA_EXE% %MAVEN_OPTS% -classpath %_EL_WRAPPER_JAR% "-Dmaven.home=%_EL_MAVEN_HOME%" "-Dmaven.multiModuleProjectDirectory=%_EL_PROJECT_DIR%" org.apache.maven.wrapper.MavenWrapperMain %*
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%
exit /B %ERROR_CODE%
