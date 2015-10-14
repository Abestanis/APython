APP_ABI := all
# The minimum API version we support is 8
APP_PLATFORM := android-8

# Workaround for MIPS toolchain linker being unable to find liblog.so dependency
APP_LDFLAGS := -llog
