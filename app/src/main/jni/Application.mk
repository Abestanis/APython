APP_ABI := all
# The minimum API version we support is 9
APP_PLATFORM := android-9

# Workaround for MIPS toolchain linker being unable to find liblog.so dependency
APP_LDFLAGS := -llog
