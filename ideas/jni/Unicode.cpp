#include <jni.h>
#include "Unicode.h"

#include <stdio.h>

JNIEXPORT void JNICALL
Java_Unicode_stringAtC(JNIEnv *env, jobject obj,
		       jstring str, jbyteArray bytes) {
  fprintf(stdout, "Unicode length: %i\n", env->GetStringLength(str));
  fprintf(stdout, "UTF length: %i\n", env->GetStringUTFLength(str));
  const char *cstr = env->GetStringUTFChars(str, NULL);
  if (cstr == NULL) return;
  fprintf(stdout, "%s\n", cstr);
  env->ReleaseStringUTFChars(str, cstr);
  
  const jint bytesLen = env->GetArrayLength(bytes);
  fprintf(stdout, "Byte array length: %i\n", bytesLen);
  char cbytes[bytesLen+1];
  env->GetByteArrayRegion(bytes, 0, bytesLen, (jbyte*)cbytes);
  cbytes[bytesLen] = '\0';
  fprintf(stdout, "%s\n", cbytes);
}
