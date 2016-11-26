#ifndef PYTHON_EXECUTABLE_H
#define PYTHON_EXECUTABLE_H
#ifdef __cplusplus
extern "C" {
#endif

#define LOG_ERROR(msg, args...) fprintf(stderr, "fatal: " msg "\n", ##args)

int main(int argc, char** argv);

#ifdef __cplusplus
}
#endif
#endif // PYTHON_EXECUTABLE_H
