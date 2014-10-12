#!/bin/sh
CP=conf/:classes/:lib/*
SP=src/java/

/bin/mkdir -p classes/

javac -sourcepath $SP -classpath $CP -d classes/ src/java/nxt/*.java src/java/nxt/*/*.java src/java/tzr/util/NSCAssets/*.java|| exit 1

/bin/rm -f tzr.jar 

jar cfm tzr.jar MANIFEST.MF -C classes . || exit 1

/bin/rm -rf classes

echo "tzr.jar generated successfully"
