@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------
@IF "%__MVNW_ARG0_NAME__%"=="" (SET "BASE_DIR=%~dp0") ELSE SET "BASE_DIR=%__MVNW_ARG0_NAME__%"
@SET MAVEN_PROJECTBASEDIR=%BASE_DIR%
@SET WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
@SET DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

@SET JAVA_HOME_ORIGINAL=%JAVA_HOME%

@FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties") DO (
    @IF "%%A"=="distributionUrl" SET "DISTRIBUTION_URL=%%B"
)

@IF NOT EXIST %WRAPPER_JAR% (
    @ECHO Downloading Maven wrapper...
    @powershell -Command "&{[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%WRAPPER_JAR%'}"
)

@SET MAVEN_CMD_LINE_ARGS=%*

@"%JAVA_HOME%\bin\java.exe" ^
    -classpath %WRAPPER_JAR% ^
    %WRAPPER_LAUNCHER% ^
    %MAVEN_CMD_LINE_ARGS%
