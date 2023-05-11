#include <incShared.h>

int binc(int n) {
  static int t = 0;
  t++;
  return n+t;
}
