#include <jni.h>

#include "incShared.h"
#include <stdio.h>

JNIEXPORT jint JNICALL
Java_NativeInc1_inc(JNIEnv *env, jobject obj, jint n) {
  return binc(n);
}
