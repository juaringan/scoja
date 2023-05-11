#!/bin/sh

CURRENT=$(dirname $0)
SCOJA_HOME=${SCOJA_HOME:-${CURRENT}}

if [ -f ${SCOJA_HOME}/scoja.jar ]; then
    CLASSPATH=""
    #for jar in ${SCOJA_HOME}/*.jar; do
    #	CLASSPATH="${CLASSPATH}:${jar}"
    #done
    for jar in scoja.jar scoja-cc.jar scoja-compression.jar scoja-rpc.jar \
      scoja-beep.jar jython.jar; do
        CLASSPATH="${CLASSPATH}:${SCOJA_HOME}/${jar}"
    done
    LIBPATH="${SCOJA_HOME}"
    SCOJA_POLICY="${SCOJA_POLICY:-${SCOJA_HOME}/scoja.policy}"
else
    BASE="${CURRENT}/../.."
    CLASSPATH="${BASE}/build/scoja"
    for jar in ${BASE}/lib/*.jar; do
	CLASSPATH="${CLASSPATH}:${jar}"
    done
    LIBPATH="${BASE}/build/obj"
    SCOJA_POLICY="${SCOJA_POLICY:-${SCOJA_HOME}/scoja.dev.policy}"
    SCOJA_HOME="${BASE}/lib"
fi
    
#LD_LIBRARY_PATH="${LD_LIBRARY_PATH}:${SCOJA_HOME}"
#echo $LD_LIBRARY_PATH

## GC Options
# jdk1.4.1
# -Xms500M -Xmx500M
# -Xincg - Usual incremental GC.
# -XX:+UseConcMarkSweepGC - This flag turns on concurrent garbage
# collection. This collector executes mostly concurrently with the
# application. It trades the utilization of processing power that
# would otherwise be available to the application for shorter garbage
# collection pause times.
# -XX:+UseParallelGC - This flag enables garbage collection to occur
# on multiple threads for better performance on multiprocessor
# machines.
#
# -verbosegc, -XX:+PrintGCDetails, -Xloggc:/var/log/scoja-gc.log
# Para más detalle en la ejecución.

#-Djava.security.manager \
#-Djava.security.policy=${SCOJA_POLICY} \

SCOJA_JVM_OPTIONS="\
-server -Xms200M -Xmx200M -XX:+UseConcMarkSweepGC \
-classpath $CLASSPATH \
-Djava.library.path=${LIBPATH} \
-Dorg.scoja.io.posix.provider=org.scoja.io.posix.PosixNative \
-Dscoja.home=${SCOJA_HOME} \
${SCOJA_JVM_OPTIONS}" 

${JAVA_HOME}/bin/java \
  ${SCOJA_JVM_OPTIONS} \
  org.scoja.server.Scoja $*
