#include <jni.h>

#include "incShared.h"
#include <stdio.h>

JNIEXPORT jint JNICALL
Java_NativeInc2_inc(JNIEnv *env, jobject obj, jint n) {
  return binc(n);
}
