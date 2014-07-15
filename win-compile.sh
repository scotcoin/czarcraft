CP="conf/;classes/;lib/*;lang/"
SP=src/java/

/bin/mkdir -p classes/

javac -sourcepath $SP -classpath $CP -d classes/ src/java/nxt/*.java src/java/nxt/*/*.java || exit 1

/bin/rm -f nfd.jar 
jar cf nfd.jar -C classes . || exit 1
/bin/rm -rf classes

echo "nfd.jar generated successfully"
