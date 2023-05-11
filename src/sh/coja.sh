#!/bin/sh

CURRENT=$(dirname $0)
SCOJA_HOME=${SCOJA_HOME:-${CURRENT}}
#echo $SCOJA_HOME

if [ -f ${SCOJA_HOME}/scoja-client.jar ]; then
    CP=""
    for jar in ${SCOJA_HOME}/*.jar; do
        if [ "$jar" != "${SCOJA_HOME}/scoja.jar" ]
        then
            CP="${CP}:${jar}"
        fi
    done
    LIBPATH="${SCOJA_HOME}"
else
    BASE="${SCOJA_HOME}/../.."
    CP="${BASE}/build/scoja"
    for jar in ${BASE}/lib/*.jar; do
        CP="${CP}:${jar}"
    done
    LIBPATH="${BASE}/build/obj"
fi

${JAVA_HOME}/bin/java \
  -server \
  -classpath "${CLASSPATH}:${CP}" \
  "-Djava.library.path=${LIBPATH}" \
  -Dorg.scoja.io.posix.provider=org.scoja.io.posix.PosixNative \
  org.scoja.client.Coja "$@"
