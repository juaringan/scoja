#include <jni.h>

#include <stdio.h>

JNIEXPORT jint JNICALL
Java_NativeInc_inc(JNIEnv *env, jobject obj, jint n) {
  return n+1;
}
