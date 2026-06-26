#!/usr/bin/env sh
mvn exec:java -Dexec.mainClass="jrgss.Game" -Dexec.args="$*"
