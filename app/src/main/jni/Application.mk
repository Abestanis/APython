APP_ABI := all
# The minimum API version we support is 14
APP_PLATFORM := android-14

# Workaround for MIPS toolchain linker being unable to find liblog.so dependency
APP_LDFLAGS := -llog

#CLANG_SANTITIZE := address

ifneq ($(CLANG_SANTITIZE),)
    APP_LDFLAGS := -fsanitize=$(CLANG_SANTITIZE)
    APP_CFLAGS := -fsanitize=$(CLANG_SANTITIZE) -g -fno-omit-frame-pointer
endif
