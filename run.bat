@echo off
mvn exec:java -Dexec.mainClass="jrgss.Game" -Dexec.args="%*"
