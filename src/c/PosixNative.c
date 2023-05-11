/*
 * Scoja: Syslog COllector in JAva
 * Copyright (C) 2003  Mario Martínez
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

#include <jni.h>
#include "PosixNative.h"

#include <errno.h>
#include <grp.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <pwd.h>
#include <string.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/un.h>
#include <unistd.h>


/*======================================================================*/
typedef struct sockaddr SA;


/*======================================================================*/
#define STRING "java/lang/String"

/*----------------------------------------------------------------------*/
/* Standard classes */
static jclass String;
static jmethodID String_new;

/*----------------------------------------------------------------------*/
/* Standard exceptions */
static jclass IOException;
static jclass SocketException;
static jclass SocketTimeoutException;

/*----------------------------------------------------------------------*/
/* Scoja exceptions */
static jclass ClosedStreamException;

/*----------------------------------------------------------------------*/
/* Scoja classes */
static jclass UserInfo;
static jmethodID UserInfo_new;

static jclass GroupInfo;
static jmethodID GroupInfo_new;

static jclass FileStat;
static jmethodID FileStat_new;

static jclass InetSocketDescription;
static jmethodID InetSocketDescription_new;

static jclass UnixSocketDescription;
static jmethodID UnixSocketDescription_new;

static jclass UnixSocketAddress;
static jmethodID UnixSocketAddress_String;

static jclass GenericDatagramPacket;
static jfieldID GenericDatagramPacket_limit;
static jfieldID GenericDatagramPacket_address;
static jfieldID GenericDatagramPacket_offset;
static jfieldID GenericDatagramPacket_data;
static jfieldID GenericDatagramPacket_length;


/*----------------------------------------------------------------------*/
static jobject JNU_FindClassWGR(JNIEnv *env, const char* clazzName) {
  jclass localClazz, globalClazz;
  localClazz = (*env)->FindClass(env, clazzName);
  if (localClazz == NULL) return NULL;
  globalClazz = (*env)->NewWeakGlobalRef(env, localClazz);
  (*env)->DeleteLocalRef(env, localClazz);
  return globalClazz;
}

JNIEXPORT jint JNICALL 
JNI_OnLoad(JavaVM *jvm, void *reserved) {
  JNIEnv *env;
  
  if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2)) {
    return JNI_ERR; /* JNI version not supported */
  }

  /*----------------------------------------------------------------------*/
  String
    = JNU_FindClassWGR(env, STRING);
  if (String == NULL) return JNI_ERR;
  String_new 
    = (*env)->GetMethodID(env, String, "<init>", "([B)V");
  if (String_new == NULL) return JNI_ERR;
  
  /*----------------------------------------------------------------------*/
  IOException
    = JNU_FindClassWGR(env, "java/io/IOException");
  if (IOException == NULL) return JNI_ERR;  
    
  SocketException
    = JNU_FindClassWGR(env, "java/net/SocketException");
  if (SocketException == NULL) return JNI_ERR;
  
  SocketTimeoutException
    = JNU_FindClassWGR(env, "java/net/SocketTimeoutException");
  if (SocketTimeoutException == NULL) return JNI_ERR;
  
  /*----------------------------------------------------------------------*/
  ClosedStreamException
    = JNU_FindClassWGR(env, "org/scoja/io/ClosedStreamException");
  if (ClosedStreamException == NULL) return JNI_ERR;
  
  /*----------------------------------------------------------------------*/
  UserInfo = JNU_FindClassWGR(env, "org/scoja/io/posix/UserInfo");
  if (UserInfo == NULL) return JNI_ERR;
  UserInfo_new
    = (*env)->GetMethodID(env, UserInfo, "<init>", "([BII[B[B[B)V");      
  if (UserInfo_new == NULL) return JNI_ERR;
  
  GroupInfo = JNU_FindClassWGR(env, "org/scoja/io/posix/GroupInfo");
  if (GroupInfo == NULL) return JNI_ERR;
  GroupInfo_new = (*env)->
    GetMethodID(env, GroupInfo, "<init>", "(L"STRING";I[L"STRING";)V");
  if (GroupInfo_new == NULL) return JNI_ERR;
    
  FileStat = JNU_FindClassWGR(env, "org/scoja/io/posix/FileStat");
  if (FileStat == NULL) return JNI_ERR;
  FileStat_new = (*env)->
    GetMethodID(env, FileStat, "<init>", "(IJIIIIIJJJJJJ)V");
  if (FileStat_new == NULL) return JNI_ERR;
  
  InetSocketDescription
    = JNU_FindClassWGR(env, "org/scoja/io/posix/InetSocketDescription");
  if (InetSocketDescription == NULL) return JNI_ERR;
  /* Throws java.net.UnknownHostException */
  InetSocketDescription_new
    = (*env)->GetMethodID(env, InetSocketDescription, "<init>", "(III)V");
  if (InetSocketDescription_new == NULL) return JNI_ERR;

  UnixSocketDescription
    = JNU_FindClassWGR(env, "org/scoja/io/posix/UnixSocketDescription");
  if (UnixSocketDescription == NULL) return JNI_ERR;
  UnixSocketDescription_new
    = (*env)->GetMethodID(env, UnixSocketDescription, "<init>", "(I[B)V");
  if (UnixSocketDescription_new == NULL) return JNI_ERR;

  UnixSocketAddress
    = JNU_FindClassWGR(env, "org/scoja/io/UnixSocketAddress");
  if (UnixSocketAddress == NULL) return JNI_ERR;
  UnixSocketAddress_String
    = (*env)->GetMethodID(env, UnixSocketAddress, "<init>", "(L"STRING";)V");
  if (UnixSocketAddress_String == NULL) return JNI_ERR;
  
  GenericDatagramPacket
    = JNU_FindClassWGR(env, "org/scoja/io/GenericDatagramPacket");
  if (GenericDatagramPacket == NULL) return JNI_ERR;
  GenericDatagramPacket_address
    = (*env)->GetFieldID(env, GenericDatagramPacket,
			 "address", "Lorg/scoja/io/SocketAddress;");    
  if (GenericDatagramPacket_address == NULL) return JNI_ERR;
  GenericDatagramPacket_offset
    = (*env)->GetFieldID(env, GenericDatagramPacket, "offset", "I");
  if (GenericDatagramPacket_offset == NULL) return JNI_ERR;
  GenericDatagramPacket_length
    = (*env)->GetFieldID(env, GenericDatagramPacket, "length", "I");
  if (GenericDatagramPacket_length == NULL) return JNI_ERR;
  GenericDatagramPacket_limit
    = (*env)->GetFieldID(env, GenericDatagramPacket, "limit", "I");
  if (GenericDatagramPacket_limit == NULL) return JNI_ERR;
  GenericDatagramPacket_data
    = (*env)->GetFieldID(env, GenericDatagramPacket, "data", "[B");    
  if (GenericDatagramPacket_data == NULL) return JNI_ERR;
  
  return JNI_VERSION_1_2;
}

JNIEXPORT void JNICALL 
JNI_OnUnload(JavaVM *jvm, void *reserved) {
  JNIEnv *env;
  if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2)) {
    return;
  }
  
  (*env)->DeleteWeakGlobalRef(env, String);
  
  (*env)->DeleteWeakGlobalRef(env, IOException);
  (*env)->DeleteWeakGlobalRef(env, SocketException);
  (*env)->DeleteWeakGlobalRef(env, SocketTimeoutException);
  
  (*env)->DeleteWeakGlobalRef(env, ClosedStreamException);
  
  (*env)->DeleteWeakGlobalRef(env, UserInfo);
  (*env)->DeleteWeakGlobalRef(env, GroupInfo);
  (*env)->DeleteWeakGlobalRef(env, FileStat);
  (*env)->DeleteWeakGlobalRef(env, InetSocketDescription);
  (*env)->DeleteWeakGlobalRef(env, UnixSocketDescription);
  (*env)->DeleteWeakGlobalRef(env, UnixSocketAddress);
  (*env)->DeleteWeakGlobalRef(env, GenericDatagramPacket);
}


/*======================================================================*/
static void
JNU_ThrowWithMessage(JNIEnv *env, jclass exception, const char *message) {
  (*env)->ThrowNew(env, exception, message);
}


static void
JNU_ThrowWithErrno(JNIEnv *env, jclass exception, int error) {
  switch (error) {
  case EBADF:
  case ENOTSOCK:
  case EPIPE:
    (*env)->ThrowNew(env, ClosedStreamException, strerror(error));
  case EWOULDBLOCK:
    /*case EAGAIN:*/
    (*env)->ThrowNew(env, SocketTimeoutException, strerror(error));
  default:
    (*env)->ThrowNew(env, exception, strerror(error));
  }
}


static jbyteArray
JNU_NewByteArray(JNIEnv *env, const char *bstr) {
  int length;
  jbyteArray barr;
  
  length = strlen(bstr);
  barr = (*env)->NewByteArray(env, length);
  if (barr == NULL) return NULL;
  (*env)->SetByteArrayRegion(env, barr, 0, length, (jbyte*)bstr);
  
  return barr;
}

static jstring
JNU_NewStringNative(JNIEnv *env, const char *bstr) {
  jbyteArray barr;
  jstring result;

  barr = JNU_NewByteArray(env, bstr);  
  if (barr == NULL) return NULL;

  result = (*env)->NewObject(env, String, String_new, barr);
  (*env)->DeleteLocalRef(env, barr);
  
  return result;
}

static jint
JNU_GetNativeFileDescriptor(JNIEnv *env, jobject fileDescriptor) {
  jclass fdClass = (*env)->GetObjectClass(env, fileDescriptor);
  jfieldID fdField = (*env)->GetFieldID(env, fdClass, "fd", "I");
  if (fdField == NULL) return -1;
  return (*env)->GetIntField(env, fileDescriptor, fdField);
}



/*======================================================================*/
/* From PosixSystem */


/*----------------------------------------------------------------------*/
/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    getCurrentUser
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_getCurrentUser
(JNIEnv *env, jobject obj) {
  return getuid();
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    getEffectiveUser
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_getEffectiveUser
(JNIEnv *env, jobject obj) {
  return geteuid();
}

/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    getCurrentGroup
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_getCurrentGroup
(JNIEnv *env, jobject obj) {
  return getgid();
}

/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    getEffectiveGroup
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_getEffectiveGroup
(JNIEnv *env, jobject obj) {
  return getegid();
}


/*----------------------------------------------------------------------*/
#define PASSWD_BUFFER_SIZE (4*1024)

static jobject
buildUserInfo(JNIEnv *env, const struct passwd *user) {
  jbyteArray name, realName, homeDirectory, shell;
  jobject result;
  
  if (user == NULL) return NULL;
  name = JNU_NewByteArray(env, user->pw_name);
  if (name == NULL) return NULL;
  realName = JNU_NewByteArray(env, user->pw_gecos);
  if (realName == NULL) return NULL;
  homeDirectory = JNU_NewByteArray(env, user->pw_dir);
  if (homeDirectory == NULL) return NULL;
  shell = JNU_NewByteArray(env, user->pw_shell);
  if (shell == NULL) return NULL;
  
  result = (*env)->
    NewObject(env, UserInfo, UserInfo_new,
	      name, user->pw_uid, user->pw_gid,
	      realName, homeDirectory, shell);
  
  return result;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    getUserInfo
 * Signature: ([B)Lorg/scoja/io/posix/UserInfo;
 */
JNIEXPORT jobject JNICALL Java_org_scoja_io_posix_PosixNative_getUserInfo___3B
(JNIEnv *env, jobject obj, jbyteArray uname) {
  const jint length = (*env)->GetArrayLength(env, uname);
  char cuname[length+1];
  struct passwd user;
  struct passwd *userResult;
  char userBuffer[PASSWD_BUFFER_SIZE];
  int error;
  
  (*env)->GetByteArrayRegion(env, uname, 0, length, (jbyte*)cuname);
  cuname[length] = '\0';
  error = getpwnam_r(cuname, &user, userBuffer, PASSWD_BUFFER_SIZE,
                     &userResult);
  if (error != 0 || userResult == NULL) return NULL;
  return buildUserInfo(env, userResult);
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    getUserInfo
 * Signature: (I)Lorg/scoja/io/posix/UserInfo;
 */
JNIEXPORT jobject JNICALL Java_org_scoja_io_posix_PosixNative_getUserInfo__I
(JNIEnv *env, jobject obj, jint uid) {
  struct passwd user;
  struct passwd *userResult;
  char userBuffer[PASSWD_BUFFER_SIZE];
  int error;
  
  error = getpwuid_r(uid, &user, userBuffer, PASSWD_BUFFER_SIZE, &userResult);
  if (error != 0 || userResult == NULL) return NULL;
  return buildUserInfo(env, userResult);
}


/*----------------------------------------------------------------------*/
#define GROUP_BUFFER_SIZE (4*1024)

static jobject
buildGroupInfo(JNIEnv *env, const struct group *grp) {
  jstring name;
  int membersNo;
  jobjectArray members;
  jobject result;
  char **m;
  int i;
  
  if (grp == NULL) return NULL;
  name = JNU_NewStringNative(env, grp->gr_name);
  if (name == NULL) return NULL;
  membersNo = 0;
  for (m = grp->gr_mem; *m != NULL; m++) membersNo++;
  members = (*env)->NewObjectArray(env, membersNo, String, NULL);
  if (members == NULL) return NULL;
  for (i = 0; i < membersNo; i++) {
    jstring member = JNU_NewStringNative(env, grp->gr_mem[i]);
    (*env)->SetObjectArrayElement(env, members, i, member);
    (*env)->DeleteLocalRef(env, member);
  }
    
  result = (*env)->
    NewObject(env, GroupInfo, GroupInfo_new, name, grp->gr_gid, members);
  
  return result;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    getGroupInfo
 * Signature: ([B)Lorg/scoja/io/posix/GroupInfo;
 */
JNIEXPORT jobject JNICALL Java_org_scoja_io_posix_PosixNative_getGroupInfo___3B
(JNIEnv *env, jobject obj, jbyteArray gname) {
  const jint length = (*env)->GetArrayLength(env, gname);
  char cgname[length+1];
  struct group grp;
  struct group *grpResult;
  char grpBuffer[GROUP_BUFFER_SIZE];
  int error;
  
  (*env)->GetByteArrayRegion(env, gname, 0, length, (jbyte*)cgname);
  cgname[length] = '\0';
  error = getgrnam_r(cgname, &grp, grpBuffer, GROUP_BUFFER_SIZE, &grpResult);
  if (error != 0 || grpResult == NULL) return NULL;
  return buildGroupInfo(env, grpResult);
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    getGroupInfo
 * Signature: (I)Lorg/scoja/io/posix/GroupInfo;
 */
JNIEXPORT jobject JNICALL Java_org_scoja_io_posix_PosixNative_getGroupInfo__I
(JNIEnv *env, jobject obj, jint gid) {
  struct group grp;
  struct group *grpResult;
  char grpBuffer[GROUP_BUFFER_SIZE];
  int error;
  
  error = getgrgid_r(gid, &grp, grpBuffer, GROUP_BUFFER_SIZE, &grpResult);
  if (error != 0 || grpResult == NULL) return NULL;
  return buildGroupInfo(env, grpResult);
}



/*======================================================================*/
/* FROM PosixFilesystem */

static jobject
buildFileStat(JNIEnv *env, struct stat *fs) {
  jobject result;
  
  result = (*env)->
    NewObject(env, FileStat, FileStat_new,
	      (jint)fs->st_dev,
	      (jlong)fs->st_ino,
	      (jint)fs->st_mode,
	      (jint)fs->st_nlink,
	      (jint)fs->st_uid,
	      (jint)fs->st_gid,
	      (jint)fs->st_rdev,
	      (jlong)fs->st_size,
	      (jlong)fs->st_blksize,
	      (jlong)fs->st_blocks,
	      (jlong)fs->st_atime,
	      (jlong)fs->st_mtime,
	      (jlong)fs->st_ctime);
  return result;  
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    getFileStat
 * Signature: ([B)Lorg/scoja/io/posix/FileStat;
 */
JNIEXPORT jobject JNICALL Java_org_scoja_io_posix_PosixNative_getFileStat___3B
(JNIEnv *env, jobject obj, jbyteArray filename) {
  const jint length = (*env)->GetArrayLength(env, filename);
  char bname[length+1];
  struct stat filestat;
  int error;
  
  (*env)->GetByteArrayRegion(env, filename, 0, length, (jbyte*)bname);
  bname[length] = '\0';
  error = stat(bname, &filestat);
  if (error != 0) {
    JNU_ThrowWithErrno(env, IOException, errno);    
    return NULL;
  }
  return buildFileStat(env, &filestat);
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    getFileStat
 * Signature: (Ljava/io/FileDescriptor;)Lorg/scoja/io/posix/FileStat;
 */
JNIEXPORT jobject JNICALL Java_org_scoja_io_posix_PosixNative_getFileStat__Ljava_io_FileDescriptor_2
(JNIEnv *env, jobject obj, jobject fileDescriptor) {
  jint fd;
  struct stat filestat;
  int error;
  
  fd = JNU_GetNativeFileDescriptor(env, fileDescriptor);
  if (fd == -1) return NULL;
  error = fstat(fd, &filestat);
  if (error != 0) {
    JNU_ThrowWithErrno(env, IOException, errno);    
    return NULL;
  }
  return buildFileStat(env, &filestat);
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    setFileMode
 * Signature: ([BI)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_setFileMode___3BI
(JNIEnv *env, jobject obj, jbyteArray filename, jint mode) {
  const jint length = (*env)->GetArrayLength(env, filename);
  char bname[length+1];
  int error;
  
  (*env)->GetByteArrayRegion(env, filename, 0, length, (jbyte*)bname);
  bname[length] = '\0';
  /*fprintf(stderr, "Changing %s to %i\n", bname, mode);*/
  error = chmod(bname, mode);
  if (error != 0) {
    JNU_ThrowWithErrno(env, IOException, errno);
  }
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    setFileMode
 * Signature: (Ljava/io/FileDescriptor;I)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_setFileMode__Ljava_io_FileDescriptor_2I
(JNIEnv *env, jobject obj, jobject fileDescriptor, jint mode) {
  jint fd;
  int error;
  
  fd = JNU_GetNativeFileDescriptor(env, fileDescriptor);
  if (fd == -1) return;
  /*fprintf(stderr, "Changing %i to %i\n", fd, mode);*/
  error = fchmod(fd, mode);
  if (error != 0) {
    JNU_ThrowWithErrno(env, IOException, errno);
  }
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    setFileOwner
 * Signature: ([BII)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_setFileOwner___3BII
(JNIEnv *env, jobject obj, jbyteArray filename, jint userid, jint groupid) {
  const jint length = (*env)->GetArrayLength(env, filename);
  char bname[length+1];
  int error;
  
  (*env)->GetByteArrayRegion(env, filename, 0, length, (jbyte*)bname);
  bname[length] = '\0';
  /*fprintf(stderr, "Changing %s to %i/%i\n", bname, userid, groupid);*/
  error = chown(bname, userid, groupid);
  if (error != 0) {
    JNU_ThrowWithErrno(env, IOException, errno);
  }
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    setFileOwner
 * Signature: (Ljava/io/FileDescriptor;II)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_setFileOwner__Ljava_io_FileDescriptor_2II
(JNIEnv *env, jobject obj, jobject fileDescriptor, jint userid, jint groupid) {
  jint fd;
  int error;
  
  fd = JNU_GetNativeFileDescriptor(env, fileDescriptor);
  if (fd == -1) return;
  /*fprintf(stderr, "Changing %i to %i/%i\n", fd, userid, groupid);*/
  error = fchown(fd, userid, groupid);
  if (error != 0) {
    JNU_ThrowWithErrno(env, IOException, errno);
  }
}


/*======================================================================*/
/* FROM PosyxIO */

static int
loadSockaddr(JNIEnv *env, struct sockaddr_un *sock, jbyteArray name) {
  jint nameLen;
  const int nameLimit = sizeof(sock->sun_path)-1;
  
  nameLen = (*env)->GetArrayLength(env, name);
  if (nameLen > nameLimit) {
    char message[100];
    snprintf(message, sizeof(message),
	     "Socket name cannot be longer than %i chars (given %i)",
	     nameLimit, nameLen);
    JNU_ThrowWithMessage(env, SocketException, message);
    return -1;
  }
  bzero(sock, sizeof(struct sockaddr_un));
  sock->sun_family = AF_LOCAL;
  (*env)->GetByteArrayRegion(env, name, 0, nameLen, 
			     (jbyte*)sock->sun_path);
  sock->sun_path[nameLen] = '\0';
  return 0;
}


/*----------------------------------------------------------------------*/
/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    close
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_close
(JNIEnv *env, jobject self, jint fd) {
  int error;

  error = close(fd);
  if (error == -1) {
    JNU_ThrowWithErrno(env, IOException, errno);
    return;
  }
}


#define OP_READ   1
#define OP_WRITE  2
/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    select
 * Signature: ([I[IIIJ)I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_select
(JNIEnv *env, jobject self, 
 jintArray fds, jintArray ops, jint length, jint maxFD, jlong timeout) {
  int i, selected;
  jint* cfds;
  jint* cops;
  fd_set readset, writeset;
  struct timeval towait, *realwait;
  
  FD_ZERO(&readset);
  FD_ZERO(&writeset);
  cfds = (*env)->GetPrimitiveArrayCritical(env, fds, NULL);
  if (cfds == NULL) return 0;
  cops = (*env)->GetPrimitiveArrayCritical(env, ops, NULL);
  if (cops == NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, fds, cfds, 0);
    return 0;
  }
  for (i = 0; i < length; i++) {
    /*fprintf(stderr, "C: Adding to select: %i\n", cfds[i]); fflush(stderr);*/
    if ((cops[i] & OP_READ) != 0) FD_SET(cfds[i], &readset);
    if ((cops[i] & OP_WRITE) != 0) FD_SET(cfds[i], &writeset);
  }
  (*env)->ReleasePrimitiveArrayCritical(env, fds, cfds, 0);
  (*env)->ReleasePrimitiveArrayCritical(env, ops, cops, 0);
  
  if (timeout == 0) {
    realwait = NULL;
  } else {
    realwait = &towait;
    if (timeout == -1) {
      towait.tv_sec = 0;
      towait.tv_usec = 0;
    } else {
      towait.tv_sec = timeout / 1000;
      towait.tv_usec = 1000 * (timeout % 1000);
    }
  }
  
  selected = select(maxFD+1, &readset, &writeset, NULL, realwait);
  if (selected == -1) {
    JNU_ThrowWithErrno(env, SocketException, errno);    
    return -1;
  }
  
  cfds = (*env)->GetPrimitiveArrayCritical(env, fds, NULL);
  if (cfds == NULL) return 0;
  cops = (*env)->GetPrimitiveArrayCritical(env, ops, NULL);
  if (cops == NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, fds, cfds, 0);
    return 0;
  }
  for (i = 0; i < length; i++) {
    cops[i] = 0;
    if (FD_ISSET(cfds[i], &readset)) cops[i] |= OP_READ;
    if (FD_ISSET(cfds[i], &writeset)) cops[i] |= OP_WRITE;
  }
  (*env)->ReleasePrimitiveArrayCritical(env, fds, cfds, 0);
  (*env)->ReleasePrimitiveArrayCritical(env, ops, cops, 0);
  
  return selected;
}


/*----------------------------------------------------------------------*/
/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    read
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_read__I
(JNIEnv *env, jobject self, jint fd) {
  int readed;
  unsigned char bdata;
  
  readed = read(fd, &bdata, 1);
  if (readed == -1) {
    JNU_ThrowWithErrno(env, IOException, errno);
    return -1;
  }
  return (readed == 0) ? -1 : (jint)bdata;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    read
 * Signature: (I[BII)I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_read__I_3BII
(JNIEnv *env, jobject self, jint fd, jbyteArray data, jint off, jint len) {
  jbyte bdata[len];
  int readed;
  
  readed = read(fd, bdata, len);
  if (readed == -1) {
    JNU_ThrowWithErrno(env, IOException, errno);
    return -1;
  }
  (*env)->SetByteArrayRegion(env, data, off, readed, bdata);
  return (readed == 0) ? -1 : readed;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    write
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_write__II
(JNIEnv *env, jobject self, jint fd, jint data) {
  unsigned char bdata;
  int written;
  
  bdata = (unsigned char)data;
  written = write(fd, &bdata, 1);
  if (written == -1) {
    JNU_ThrowWithErrno(env, IOException, errno);
    return -1;
  }
  return written;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    write
 * Signature: (I[BII)I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_write__I_3BII
(JNIEnv *env, jobject self, jint fd, jbyteArray data, jint off, jint len) {
  jbyte bdata[len];
  int written;
  
  (*env)->GetByteArrayRegion(env, data, off, len, bdata);
  written = write(fd, bdata, len);
  if (written == -1) {
    JNU_ThrowWithErrno(env, IOException, errno);
    return -1;
  }
  return written;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    newPipe
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_org_scoja_io_posix_PosixNative_newPipe
(JNIEnv *env, jobject self) {
  int error;
  int filedes[2];
  
  error = pipe(filedes);
  if (error == -1) {
    JNU_ThrowWithErrno(env, IOException, errno);
    return -1;
  }
  return (((jlong)filedes[0]) << 32) | filedes[1];
}


/*----------------------------------------------------------------------*/
/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    newInetDatagram
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_newInetDatagram
(JNIEnv *env, jobject self) {
  int sockfd = socket(AF_INET, SOCK_DGRAM, 0);
  if (sockfd == -1) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return -1;
  }
  return sockfd;
}

/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    newInetStream
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_newInetStream
(JNIEnv *env, jobject self) {
  int sockfd = socket(AF_INET, SOCK_STREAM, 0);
  if (sockfd == -1) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return -1;
  }
  return sockfd;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    newUnixDatagram
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_newUnixDatagram
(JNIEnv *env, jobject self) {
  int sockfd = socket(AF_LOCAL, SOCK_DGRAM, 0);
  if (sockfd == -1) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return -1;
  }
  return sockfd;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    newUnixStream
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_newUnixStream
(JNIEnv *env, jobject self) {
  int sockfd = socket(AF_LOCAL, SOCK_STREAM, 0);
  if (sockfd == -1) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return -1;
  }
  return sockfd;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    bind
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_bind__III
(JNIEnv *env, jobject self, jint fd, jint ip, jint port) {
  struct sockaddr_in serverAddress;
  int error;
  
  bzero(&serverAddress, sizeof(serverAddress));
  serverAddress.sin_family = AF_INET;
  serverAddress.sin_addr.s_addr = htonl(ip);
  serverAddress.sin_port = htons(port);
  error = bind(fd, (SA*)&serverAddress, sizeof(serverAddress));
  if (error != 0) {
    JNU_ThrowWithMessage(env, SocketException, strerror(errno));
    return;
  }
}

  
/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    bind
 * Signature: (I[B)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_bind__I_3B
(JNIEnv *env, jobject self, jint fd, jbyteArray name) {
  struct sockaddr_un serverAddress;
  int error;
  
  error = loadSockaddr(env, &serverAddress, name);
  if (error != 0) return;
  error = bind(fd, (SA*)&serverAddress, sizeof(serverAddress));
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return;
  }
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    listen
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_listen
(JNIEnv *env, jobject self, jint fd, jint incomingQueueLimit) {
  int error;
  
  error = listen(fd, incomingQueueLimit);
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return;
  }  
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    connect
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_connect__III
(JNIEnv *env, jobject self, jint fd, jint ip, jint port) {
  struct sockaddr_in serverAddress;
  int error;
  
  bzero(&serverAddress, sizeof(serverAddress));
  serverAddress.sin_family = AF_INET;
  serverAddress.sin_addr.s_addr = htonl(ip);
  serverAddress.sin_port = htons(port);
  error = connect(fd, (SA*)&serverAddress, sizeof(serverAddress));
  if (error != 0) {
    JNU_ThrowWithMessage(env, SocketException, strerror(errno));
    return;
  }
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    connect
 * Signature: (I[B)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_connect__I_3B
(JNIEnv *env, jobject self, jint fd, jbyteArray name) {
  struct sockaddr_un serverAddress;
  int error;
  
  error = loadSockaddr(env, &serverAddress, name);
  if (error != 0) return;
  error = connect(fd, (SA*)&serverAddress, sizeof(serverAddress));
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return;
  }  
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    acceptInet
 * Signature: (I)Lorg/scoja/io/posix/InetSocketDescription;
 */
JNIEXPORT jobject JNICALL Java_org_scoja_io_posix_PosixNative_acceptInet
(JNIEnv *env, jobject self, jint fd) {
  int connfd;
  struct sockaddr_in clientAddress;
  socklen_t clientLength;
  jobject desc;
  
  clientLength = sizeof(clientAddress);
  /*fprintf(stdout, "C: before accept\n"); fflush(stdout);*/
  connfd = accept(fd, (SA*)&clientAddress, &clientLength);
  /*fprintf(stdout, "C: after accept\n"); fflush(stdout);*/
  if (connfd < 0) {
    /*fprintf(stdout, "C: accept error\n"); fflush(stdout);*/
    JNU_ThrowWithErrno(env, IOException, errno);
    return NULL;
  }
  desc = (*env)->NewObject(env,
                           InetSocketDescription, InetSocketDescription_new,
                           (jint)connfd, 
                           ntohl(clientAddress.sin_addr.s_addr),
                           ntohs(clientAddress.sin_port));
  return desc;
}
  
  
/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    acceptUnix
 * Signature: (I)Lorg/scoja/io/UnixSocketDescription;
 */
JNIEXPORT jobject JNICALL Java_org_scoja_io_posix_PosixNative_acceptUnix
(JNIEnv *env, jobject self, jint fd) {
  int connfd;
  struct sockaddr_un clientAddress;
  socklen_t clientLength;
  jbyteArray clientName;
  jobject desc;
  
  clientLength = sizeof(clientAddress);
  /*fprintf(stdout, "C: before accept\n"); fflush(stdout);*/
  connfd = accept(fd, (SA*)&clientAddress, &clientLength);
  /*fprintf(stdout, "C: after accept\n"); fflush(stdout);*/
  if (connfd < 0) {
    /*fprintf(stdout, "C: accept error\n"); fflush(stdout);*/
    JNU_ThrowWithErrno(env, IOException, errno);
    return NULL;
  }
  clientName = JNU_NewByteArray(env, clientAddress.sun_path);
  desc = (*env)->NewObject(env,
                           UnixSocketDescription, UnixSocketDescription_new,
                           (jint)connfd, clientName);
  return desc;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    send
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_send__II
(JNIEnv *env, jobject self, jint fd, jint data) {
  unsigned char bdata;
  int sended;
  
  bdata = (unsigned char)data;
  sended = send(fd, &bdata, 1, MSG_NOSIGNAL);
  if (sended == -1) {
    JNU_ThrowWithErrno(env, IOException, errno);
    return -1;
  }
  return sended;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    send
 * Signature: (I[BII)I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_send__I_3BII
(JNIEnv *env, jobject self, jint fd, jbyteArray data, jint off, jint len) {
  jbyte bdata[len];
  int sended;
  
  (*env)->GetByteArrayRegion(env, data, off, len, bdata);
  sended = send(fd, bdata, len, MSG_NOSIGNAL);
  if (sended == -1) {
    JNU_ThrowWithErrno(env, IOException, errno);
    return -1;
  }
  return sended;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    sendTo
 * Signature: (I[BII[B)I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_sendTo
(JNIEnv *env, jobject self, jint fd, jbyteArray data, jint off, jint len,
 jbyteArray destName) {
  struct sockaddr_un destAddress;
  jbyte bdata[len];
  int error, sended;
  
  error = loadSockaddr(env, &destAddress, destName);
  if (error != 0) return -1;
  (*env)->GetByteArrayRegion(env, data, off, len, bdata);
  sended = sendto(fd, bdata, len, MSG_NOSIGNAL,
		  (SA*)&destAddress, sizeof(destAddress));
  if (sended == -1) {
    JNU_ThrowWithErrno(env, IOException, errno);
    return -1;
  }
  return sended;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    receive
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_receive__I
(JNIEnv *env, jobject self, jint fd) {
  int readed;
  unsigned char bdata;
  
  readed = recv(fd, &bdata, 1, MSG_NOSIGNAL);
  if (readed == -1) {
    JNU_ThrowWithErrno(env, IOException, errno);
    return -1;
  }
  return (readed == 0) ? -1 : (jint)bdata;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    receive
 * Signature: (I[BII)I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_receive__I_3BII
(JNIEnv *env, jobject self, jint fd, jbyteArray data, jint off, jint len) {
  jbyte bdata[len];
  int readed;
  
  readed = recv(fd, bdata, len, MSG_NOSIGNAL);
  if (readed == -1) {
    JNU_ThrowWithErrno(env, IOException, errno);
    return -1;
  }
  (*env)->SetByteArrayRegion(env, data, off, readed, bdata);
  return (readed == 0) ? -1 : readed;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    receiveFrom
 * Signature: (ILorg/scoja/io/GenericDatagramPacket;)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_receiveFrom__ILorg_scoja_io_GenericDatagramPacket_2
(JNIEnv *env, jobject self, jint fd, jobject packet) {
  const int limit
    = (*env)->GetIntField(env, packet, GenericDatagramPacket_limit);
  jbyte bdata[limit];
  struct sockaddr_un srcAddress;
  socklen_t srcLength;
  int readed;
  
  bzero(&srcAddress, sizeof(srcAddress));
  srcLength = sizeof(srcAddress);
 
  readed = recvfrom(fd, bdata, limit, MSG_NOSIGNAL,
		    (SA*)&srcAddress, &srcLength);
  if (readed == -1) {
    JNU_ThrowWithErrno(env, IOException, errno);
    return;
  } else {
    jstring srcName;
    jobject src;
    jint offset;
    jbyteArray data;
    
    srcName = JNU_NewStringNative(env, srcAddress.sun_path);
    if (srcName == NULL) return;
    src = (*env)->NewObject(env, UnixSocketAddress, UnixSocketAddress_String,
			    srcName);
    if (src == NULL) return;
    (*env)->SetObjectField(env, packet, GenericDatagramPacket_address, src);
    offset = (*env)->GetIntField(env, packet, GenericDatagramPacket_offset);
    data = (jbyteArray)(*env)
      ->GetObjectField(env, packet, GenericDatagramPacket_data);
    (*env)->SetByteArrayRegion(env, data, offset, readed, bdata);
    (*env)->SetIntField(env, packet, GenericDatagramPacket_length, readed);
  }
}

/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    shutdown
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_shutdown
(JNIEnv *env, jobject self, jint fd, jint what) {
  int howto;
  int error;
  
  if (what == 1) howto = SHUT_RD;
  else if (what == 2) howto = SHUT_WR;
  else if (what == 3) howto = SHUT_RDWR;
  else {
    JNU_ThrowWithMessage(env, IOException, "Unknown shutdown howto");
    return;
  }
  error = shutdown(fd, howto);
  if (error == -1) {
    JNU_ThrowWithErrno(env, IOException, errno);
    return;
  }
}


/*----------------------------------------------------------------------*/
/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    setDebug
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_setDebug
(JNIEnv *env, jobject self, jint fd, jboolean on) {
  int onVal = on;
  int error;
  
  error = setsockopt(fd, SOL_SOCKET, SO_DEBUG, &on, sizeof(onVal));
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return;
  }
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    getDebug
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_scoja_io_posix_PosixNative_getDebug
(JNIEnv *env, jobject self, jint fd) {
  int on;
  socklen_t vs = sizeof(on);
  int error;
  
  error = getsockopt(fd, SOL_SOCKET, SO_BROADCAST, &on, &vs);
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return JNI_FALSE;
  }
  return on != 0;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    setBroadcast
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_setBroadcast
(JNIEnv *env, jobject self, jint fd, jboolean on) {
  int onVal = on;
  int error;
  
  error = setsockopt(fd, SOL_SOCKET, SO_BROADCAST, &onVal, sizeof(onVal));
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return;
  }
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    getBroadcast
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_scoja_io_posix_PosixNative_getBroadcast
(JNIEnv *env, jobject self, jint fd) {
  int on;
  socklen_t vs = sizeof(on);
  int error;
  
  error = getsockopt(fd, SOL_SOCKET, SO_BROADCAST, &on, &vs);
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return JNI_FALSE;
  }
  return on != 0;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    setKeepAlive
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_setKeepAlive
(JNIEnv *env, jobject self, jint fd, jboolean on) {
  int onVal = on;
  int error;
  
  error = setsockopt(fd, SOL_SOCKET, SO_KEEPALIVE, &onVal, sizeof(onVal));
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return;
  }
}

  
/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    getKeepAlive
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_scoja_io_posix_PosixNative_getKeepAlive
(JNIEnv *env, jobject self, jint fd) {
  int on;
  socklen_t vs = sizeof(on);
  int error;
  
  error = getsockopt(fd, SOL_SOCKET, SO_KEEPALIVE, &on, &vs);
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return JNI_FALSE;
  }
  return on != 0;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    setOOBInline
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_setOOBInline
(JNIEnv *env, jobject self, jint fd, jboolean on) {
  int onVal = on;
  int error;
  
  error = setsockopt(fd, SOL_SOCKET, SO_OOBINLINE, &onVal, sizeof(onVal));
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return;
  }
}

/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    getOOBInline
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_scoja_io_posix_PosixNative_getOOBInline
(JNIEnv *env, jobject self, jint fd) {
  int on;
  socklen_t vs = sizeof(on);
  int error;
  
  error = getsockopt(fd, SOL_SOCKET, SO_OOBINLINE, &on, &vs);
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return JNI_FALSE;
  }
  return on != 0;
}

  
/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    setReadTimeout
 * Signature: (IJ)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_setReadTimeout
(JNIEnv *env, jobject self, jint fd, jlong timeout) {
  struct timeval tv;
  int error;
  
  tv.tv_sec = timeout / 1000;
  tv.tv_usec = (timeout % 1000) * 1000;
  error = setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return;
  }
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    getReadTimeout
 * Signature: (I)I
 */
JNIEXPORT jlong JNICALL Java_org_scoja_io_posix_PosixNative_getReadTimeout
(JNIEnv *env, jobject self, jint fd) {
  struct timeval tv;
  socklen_t vs = sizeof(tv);
  int error;
  
  error = getsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &tv, &vs);
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return 0;
  }
  return tv.tv_sec*1000 + tv.tv_usec/1000;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    setReceiveBufferSize
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_setReceiveBufferSize
(JNIEnv *env, jobject self, jint fd, jint size) {
  int sizeVal = size;
  int error;
  
  error = setsockopt(fd, SOL_SOCKET, SO_RCVBUF, &sizeVal, sizeof(sizeVal));
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return;
  }
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    getReceiveBufferSize
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_getReceiveBufferSize
(JNIEnv *env, jobject self, jint fd) {
  int size;
  socklen_t vs = sizeof(size);
  int error;
  
  error = getsockopt(fd, SOL_SOCKET, SO_RCVBUF, &size, &vs);
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return 0;
  }
  return size;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    setReuseAddress
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_setReuseAddress
(JNIEnv *env, jobject self, jint fd, jboolean reuse) {
  int reuseVal = reuse;
  int error;
  
  error = setsockopt(fd, SOL_SOCKET,
                     SO_REUSEADDR, &reuseVal, sizeof(reuseVal));
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return;
  }
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    getReuseAddress
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_scoja_io_posix_PosixNative_getReuseAddress
(JNIEnv *env, jobject self, jint fd) {
  int reuse;
  socklen_t vs = sizeof(reuse);
  int error;
  
  error = getsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &reuse, &vs);
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return JNI_FALSE;
  }
  return reuse != 0;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    setSendBufferSize
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_setSendBufferSize
(JNIEnv *env, jobject self, jint fd, jint size) {
  int sizeVal = size;
  int error;
  
  error = setsockopt(fd, SOL_SOCKET, SO_SNDBUF, &sizeVal, sizeof(sizeVal));
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return;
  }
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    getSendBufferSize
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_getSendBufferSize
(JNIEnv *env, jobject self, jint fd) {
  int size;
  socklen_t vs = sizeof(size);
  int error;
  
  error = getsockopt(fd, SOL_SOCKET, SO_RCVBUF, &size, &vs);
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return 0;
  }
  return size;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    setSoLinger
 * Signature: (IZI)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_setSoLinger
(JNIEnv *env, jobject self, jint fd, jboolean on, jint timeout) {
  struct linger linger;
  int error;
  
  linger.l_onoff = on;
  linger.l_linger = timeout;
  error = setsockopt(fd, SOL_SOCKET, SO_LINGER, &linger, sizeof(linger));
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return;
  }
}

/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    getSoLinger
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_scoja_io_posix_PosixNative_getSoLinger
(JNIEnv *env, jobject self, jint fd) {
  struct linger linger;
  socklen_t vs = sizeof(linger);
  int error;
  
  error = getsockopt(fd, SOL_SOCKET, SO_LINGER, &linger, &vs);
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return 0;
  }
  return linger.l_onoff ? linger.l_linger : -1;
}


/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    setTcpNoDelay
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_org_scoja_io_posix_PosixNative_setTcpNoDelay
(JNIEnv *env, jobject self, jint fd, jboolean on) {
  int onVal = on;
  int error;
  
  error = setsockopt(fd, IPPROTO_TCP, TCP_NODELAY, &onVal, sizeof(onVal));
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return;
  }
}

/*
 * Class:     org_scoja_io_posix_PosixNative
 * Method:    getTcpNoDelay
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_org_scoja_io_posix_PosixNative_getTcpNoDelay
(JNIEnv *env, jobject self, jint fd) {
  int on;
  socklen_t vs = sizeof(on);
  int error;
  
  error = getsockopt(fd, IPPROTO_TCP, TCP_NODELAY, &on, &vs);
  if (error != 0) {
    JNU_ThrowWithErrno(env, SocketException, errno);
    return JNI_FALSE;
  }
  return on != 0;
}
