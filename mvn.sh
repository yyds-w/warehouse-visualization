#!/usr/bin/env bash
# 直接用 java 启动 Maven 主类，绕过 Git Bash 下 mvn 脚本的 ClassNotFound 问题。
# 用法: ./mvn.sh compile | ./mvn.sh spring-boot:run | ./mvn.sh -DskipTests package
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/warehouse-backend"
BACKEND_DIR_WIN="$(cygpath -w "$BACKEND_DIR")"
cd "$BACKEND_DIR"
exec java -classpath "D:\IDEA\MAVEN\apache-maven-3.9.9\boot\plexus-classworlds-2.8.0.jar" \
  -Dclassworlds.conf="D:\IDEA\MAVEN\apache-maven-3.9.9\bin\m2.conf" \
  -Dmaven.home="D:\IDEA\MAVEN\apache-maven-3.9.9" \
  -Dmaven.multiModuleProjectDirectory="$BACKEND_DIR_WIN" \
  org.codehaus.plexus.classworlds.launcher.Launcher \
  -s "$BACKEND_DIR_WIN\\settings.xml" "$@"
