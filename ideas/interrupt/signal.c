#include <stdlib.h>
#include <signal.h>

int main(int argc, char** args) {
  struct  sigaction action;
  action.sa_handler = (void (*)(int))SIG_IGN;
  sigemptyset(&action.sa_mask);
  action.sa_flags = 0;
  sigaction(SIGRTMIN + 29, &action, NULL);
}
