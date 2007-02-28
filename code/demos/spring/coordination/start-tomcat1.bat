@echo off

rem
rem  All content copyright (c) 2003-2006 Terracotta, Inc.,
rem  except as may otherwise be noted in a separate copyright notice.
rem  All rights reserved.
rem

rem
rem samples\spring\coordination
rem
rem Environment variables required by dso-env helper script:
rem  JAVA_HOME: root of Java Development Kit installation
rem  TC_INSTALL_DIR: root of Terracotta installation
rem
rem Arguments to dso-env helper script:
rem  -q: do not print value of TC_JAVA_OPTS
rem  tc-config.xml: path to DSO config file
rem
rem Environment variable set by dso-env helper script:
rem  TC_JAVA_OPTS: Java options needed to activate DSO
rem

setlocal
cd %~d0%~p0
set TC_INSTALL_DIR=..\..\..
set CATALINA_HOME=%TC_INSTALL_DIR%\vendors\tomcat5.5
if not exist "%JAVA_HOME%" set JAVA_HOME=%TC_INSTALL_DIR%\jre
call "%TC_INSTALL_DIR%\bin\dso-env.bat" -q tc-config.xml
set JAVA_OPTS=%TC_JAVA_OPTS% %JAVA_OPTS% -Dcounter.log.prefix="CounterService-Tomcat-Node-1:"
set CATALINA_BASE=tomcat1
start "terracotta for spring: thread coordination sample: tomcat server node 1" "%CATALINA_HOME%\bin\catalina.bat" run
endlocal
