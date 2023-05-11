 README
========

 Dependencies
--------------

Scoja project has the following modules
(with theirs dependencies in parenthesis):
  common
  beep (common)
  rpc (common)
  compression (common, scoja)
  scoja (common, beep, rcp, compression)
  
There is a circular dependency between compression and scoja modules.
compression module depends only on runtime on scoja-client.jar produced
at scoja module.
The building script of compression doesn't fail if it doesn't find
scoja-client.jar.
So, the compilation order
  common, beep, rpc, compression, scoja
is correct.

Scoja has serveral external dependencies:
  ant
  gmake
  m4
  gcc
  javacc
  jython

Two modules have JNI components.
To compile them, jni.h is needed.
Because there is no standard location for jni.h, 
the compilation script (Makefile) search from JAVA_HOME.
So this environment variable must point to the root of the Java instalation.

We will suppose that
  ant, make, m4 and gcc are in the path
  javacc is in /usr/share/java 
  java is in /usr/lib/jvm/java-6-sun
  jython is in /opt/jython
To get a full building from scratch, execute the following commands:

  export JAVA_HOME=/usr/lib/jvm/java-6-sun

  cd common
  echo "javacc.home=/usr/share/java" > build.properties
  ant compile-ant
  CLASSPATH=build/classes ant distrib-bin

  cd ../beep
  ant distrib-bin

  cd ../rpc
  echo "javacc.home=/usr/share/java" > build.properties
  ant distrib-bin

  cd ../compression
  ant distrib-bin

  cd ../scoja
  echo "jython.home=/opt/jython" > build.properties
  ant distrib-bin

  cd ../compression
  ant distrib-bin


 Compilation
-------------

Copy build.properties.suggestion to build.properties
and change it to fit your compilation environment.
