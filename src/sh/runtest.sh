#!/bin/sh

CURRENT=$(dirname $0)
SCOJA_HOME=${SCOJA_HOME:-${CURRENT}}
BASE="${SCOJA_HOME}/../.."
CLASSPATH="${BASE}/build/scoja\
:${BASE}/build/test/common\
:${BASE}/build/test/human\
:${BASE}/build/test/junit"
for jar in ${BASE}/lib/*.jar; do
    CLASSPATH="${CLASSPATH}:${jar}"
done
LIBPATH="${BASE}/build/obj"
    
#echo $CLASSPATH

${JAVA_HOME}/bin/java \
  -server -Xms500M -Xmx500M -XX:+UseConcMarkSweepGC \
  -classpath $CLASSPATH \
  -Djava.library.path=${LIBPATH} \
  -Dorg.scoja.io.posix.provider=org.scoja.io.posix.PosixNative \
  "$@"
