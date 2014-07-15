CP=nfd.jar:lib/*:conf:lang
SP=src/java/

/bin/rm -rf html/doc/*

javadoc -quiet -sourcepath $SP -classpath $CP -protected -splitindex -subpackages nxt -d html/doc/
