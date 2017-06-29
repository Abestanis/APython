APP_ABI := all
# The minimum API version we support is 14
APP_PLATFORM := android-14

# Workaround for MIPS toolchain linker being unable to find liblog.so dependency
APP_LDFLAGS := -llog
