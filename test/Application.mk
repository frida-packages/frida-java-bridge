include config.mk

android_api_major := $(firstword $(subst ., ,$(ANDROID_API_LEVEL)))
ndk_max_api := $(shell sed -n 's/^[[:space:]]*"max":[[:space:]]*\([0-9]*\).*/\1/p' $(ANDROID_NDK_ROOT)/meta/platforms.json 2>/dev/null)

APP_ABI := $(ANDROID_ABI)
APP_PLATFORM := android-$(firstword $(sort $(android_api_major) $(ndk_max_api)))
APP_STL := c++_static
APP_BUILD_SCRIPT := Android.mk
