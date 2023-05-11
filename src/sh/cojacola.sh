#!/bin/sh

CURRENT=$(dirname $0)
SCOJA_HOME=${SCOJA_HOME:-${CURRENT}}
#echo $SCOJA_HOME

if [ -f ${SCOJA_HOME}/scoja.jar ]; then
    CLASSPATH=""
    for jar in ${SCOJA_HOME}/*.jar; do
	CLASSPATH="${CLASSPATH}:${jar}"
    done
    LIBPATH="${SCOJA_HOME}"
    LOGCONF="${SCOJA_HOME}/cojacola.logconf"
else
    BASE="${SCOJA_HOME}/../.."
    CLASSPATH="${BASE}/build/scoja"
    for jar in ${BASE}/lib/*.jar; do
	CLASSPATH="${CLASSPATH}:${jar}"
    done
    LIBPATH="${BASE}/build/obj"
    LOGCONF="${SCOJA_HOME}/cojacola.logconf"
fi
    
${JAVA_HOME}/bin/java \
  -server \
  -classpath "${CLASSPATH}" \
  "-Djava.library.path=${LIBPATH}" \
  -Dorg.scoja.io.posix.provider=org.scoja.io.posix.PosixNative \
  "-Djava.util.logging.config.file=${LOGCONF}" \
  org.scoja.popu.cojacola.CojaCola $*
