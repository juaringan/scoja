.SUFFIXES:
.SUFFIXES: .c .o .so

java_home = $(JAVA_HOME)/
jnimd = $(shell find $(java_home) -name jni_md.h)
os = $(jnimd:/jni_md.h=)

srcs_dir = src/c/
build_dir = build/obj/
libs_dir = lib/

object_flags = -Wall -c -O2 -fno-strict-aliasing -fPIC
lib_flags = -Wall -shared -W1 -lc -fPIC
include_flags = -I$(java_home)/include -I$(os) -Ibuild/include

linker = gcc


all: native_c
native_c: build/obj/libPosixNative.so


build/obj/lib%.so: build/obj/%.o
	$(linker) $(lib_flags) -o $@ $^


build/obj/%.o: src/c/%.c
	gcc $(include_flags) $(object_flags) -o $@ $^

build/obj/%.o: src/c/%.cpp
	gcc $(include_flags) $(object_flags) -o $@ $^

clean:
	rm -f *.o *~ *.so
