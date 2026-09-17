@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM   http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "BASE_DIR=%~dp0") ELSE SET "BASE_DIR=%__MVNW_ARG0_NAME__%"

@SET MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%
@IF NOT "%MAVEN_PROJECTBASEDIR%"=="" GOTO endDetectBaseDir

@SET EXEC_DIR=%CD%
@SET WD=%EXEC_DIR%
:findBaseDir
@IF EXIST "%WD%"\.mvn GOTO baseDirFound
@cd ..
@IF "%WD%"=="%CD%" GOTO baseDirNotFound
@SET "WD=%CD%"
@GOTO findBaseDir

:baseDirFound
SET "MAVEN_PROJECTBASEDIR=%WD%"
@cd "%EXEC_DIR%"
@GOTO endDetectBaseDir

:baseDirNotFound
SET "MAVEN_PROJECTBASEDIR=%EXEC_DIR%"
@cd "%EXEC_DIR%"

:endDetectBaseDir

@SET WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
@SET DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar

@IF NOT EXIST %WRAPPER_JAR% (
    @ECHO Downloading Maven wrapper...
    @powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%WRAPPER_JAR%'}"
)

@IF "%MVNW_VERBOSE%"=="true" (
    @ECHO MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR%
)

@SET MAVEN_CMD_LINE_ARGS=%*

@"%JAVA_HOME%\bin\java.exe" ^
    "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
    -classpath %WRAPPER_JAR% ^
    %WRAPPER_LAUNCHER% ^
    %MAVEN_CMD_LINE_ARGS%
