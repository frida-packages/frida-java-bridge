include config.mk

build_os_arch := $(shell uname -s | tr -s "[A-Z]" "[a-z]")-$(shell uname -m)
jvm_arch := $(shell uname -m)
JAVA_HOME ?= $(shell dirname $$(dirname $$(readlink -f $$(which javac))))
jvm_lib := $(firstword $(wildcard $(JAVA_HOME)/lib/server/libjvm.so $(JAVA_HOME)/jre/lib/amd64/server/libjvm.so))

android_api_major := $(firstword $(subst ., ,$(ANDROID_API_LEVEL)))
android_platform := $(if $(wildcard $(ANDROID_SDK_ROOT)/platforms/android-$(ANDROID_API_LEVEL)/android.jar),android-$(ANDROID_API_LEVEL),$(notdir $(lastword $(sort $(wildcard $(ANDROID_SDK_ROOT)/platforms/android-$(ANDROID_API_LEVEL).*)))))
android_platform := $(if $(android_platform),$(android_platform),android-$(ANDROID_API_LEVEL))
android_sysroot := $(ANDROID_NDK_ROOT)/platforms/android-$(android_api_major)/arch-$(ANDROID_ARCH)

android_deploy_prefix := /data/local/tmp
android_deploy_data_dir := $(android_deploy_prefix)/frida-java-bridge-tests
android_deploy_cache_dir := $(android_deploy_data_dir)/dalvik-cache

frida_version := 17.26.3

android_test_sources := $(wildcard re/frida/*.java)
jvm_test_sources := $(filter-out re/frida/ClassCreationTest.java re/frida/PerformTest.java,$(android_test_sources))
bridge_sources := bundle.js ../index.js $(wildcard ../lib/*.js)

android_jar := $(ANDROID_SDK_ROOT)/platforms/$(android_platform)/android.jar

android_deploy_files := \
	build/$(ANDROID_ABI)/runner \
	build/$(ANDROID_ABI)/libartpalette.so \
	build/tests-android.dex \
	build/frida-java-bridge.js

all:
	@echo "Available targets: build-android deploy-android run-android watch-android debug-android build-jvm run-jvm watch-jvm debug-jvm"

clean:
	rm -rf build

build-android: $(android_deploy_files)

deploy-android: build-android
	adb shell "mkdir -p $(android_deploy_data_dir) $(android_deploy_cache_dir)"
	adb push --sync $(android_deploy_files) $(android_deploy_data_dir)/

run-android:
	@since=$$(adb shell 'date "+%m-%d %H:%M:%S.000"' | tr -d '\r'); \
	adb shell "LD_PRELOAD=$(ANDROID_VM) LD_LIBRARY_PATH='$(APEX_LIBDIRS):$(android_deploy_data_dir)' $(android_deploy_data_dir)/runner $(RUNNER_ARGS)" || { \
		status=$$?; \
		adb logcat -d -T "$$since" -b crash || true; \
		adb logcat -d -T "$$since" -b main *:W || true; \
		exit $$status; \
	}

watch-android:
	npm run watch &
	./node_modules/.bin/chokidar \
		build/frida-java-bridge.js \
		-c "adb push --sync build/frida-java-bridge.js $(android_deploy_data_dir)/ \
			&& adb shell \"LD_PRELOAD=$(ANDROID_VM) LD_LIBRARY_PATH='$(APEX_LIBDIRS):$(android_deploy_data_dir)' $(android_deploy_data_dir)/runner $(RUNNER_ARGS)\""

debug-android: build/gdb-android.setup
	adb shell "rm -f $(android_deploy_data_dir)/debug-socket"
	adb push $(ANDROID_NDK_ROOT)/prebuilt/android-$(ANDROID_ARCH)/gdbserver/gdbserver $(android_deploy_data_dir)
	adb shell "$(android_deploy_data_dir)/gdbserver +$(android_deploy_data_dir)/debug-socket $(android_deploy_data_dir)/runner $(RUNNER_ARGS)" &
	sleep 1
	adb forward tcp:$(DEBUG_PORT) localfilesystem:$(android_deploy_data_dir)/debug-socket
	$(ANDROID_NDK_ROOT)/prebuilt/$(build_os_arch)/bin/gdb -x build/gdb-android.setup

build-jvm: build/jvm/runner build/tests-jvm.jar build/frida-java-bridge.js

run-jvm:
	@test -n "$(jvm_lib)" || { echo "Unable to locate libjvm.so under JAVA_HOME=$(JAVA_HOME)" >&2; exit 1; }
	FRIDA_JAVA_TESTS_JVM_PATH="$(jvm_lib)" build/jvm/runner $(RUNNER_ARGS)

watch-jvm:
	@test -n "$(jvm_lib)" || { echo "Unable to locate libjvm.so under JAVA_HOME=$(JAVA_HOME)" >&2; exit 1; }
	npm run watch &
	./node_modules/.bin/chokidar \
		build/frida-java-bridge.js \
		-c "FRIDA_JAVA_TESTS_JVM_PATH='$(jvm_lib)' build/jvm/runner $(RUNNER_ARGS)"

debug-jvm:
	@test -n "$(jvm_lib)" || { echo "Unable to locate libjvm.so under JAVA_HOME=$(JAVA_HOME)" >&2; exit 1; }
	FRIDA_JAVA_TESTS_JVM_PATH="$(jvm_lib)" gdb --args build/jvm/runner $(RUNNER_ARGS)

build/gdb-android.setup: build/android-symbols.stamp
	( \
		echo "set sysroot $(abspath build/android-symbols/)"; \
		echo "directory $(android_sysroot)/usr/include"; \
		echo "directory $(abspath .)"; \
		echo "file $(abspath build/obj/local/$(ANDROID_ABI)/runner)"; \
		echo "target remote :$(DEBUG_PORT)"; \
	) > $@

build/android-symbols.stamp:
	rm -rf build/android-symbols
	mkdir -p build/android-symbols/$(ANDROID_BINDIR)/ build/android-symbols/$(ANDROID_LIBDIR)/
	adb pull -a $(ANDROID_BINDIR)/linker build/android-symbols/$(ANDROID_BINDIR)/
	adb pull -a $(shell adb shell "ls $(ANDROID_LIBDIR)/*.so") build/android-symbols/$(ANDROID_LIBDIR)/; \
	touch $@

build/$(ANDROID_ABI)/libartpalette.so: build/$(ANDROID_ABI)/runner
	@test -f $@

build/$(ANDROID_ABI)/runner: runner.c artpalette.c build/obj/local/$(ANDROID_ABI)/libfrida-gumjs.a
	$(ANDROID_NDK_ROOT)/ndk-build \
		NDK_PROJECT_PATH=$$(pwd) \
		NDK_APPLICATION_MK=$$(pwd)/Application.mk \
		NDK_OUT=$$(pwd)/build/obj \
		NDK_LIBS_OUT=$$(pwd)/build \
		FRIDA_JAVA_TESTS_DATA_DIR=$(android_deploy_data_dir) \
		FRIDA_JAVA_TESTS_CACHE_DIR=$(android_deploy_cache_dir)

build/obj/local/$(ANDROID_ABI)/libfrida-gumjs.a:
	@mkdir -p $(@D)
	curl -fsSL https://github.com/frida/frida/releases/download/$(frida_version)/frida-gumjs-devkit-$(frida_version)-android-$(ANDROID_ARCH).tar.xz -o $@.tar.xz.tmp
	tar -xJf $@.tar.xz.tmp -C $(@D)
	rm -f $@.tar.xz.tmp

build/jvm/runner: runner.c build/jvm/frida-gumjs/libfrida-gumjs.a
	@mkdir -p $(@D)
	gcc -g -Wall -Werror -pthread \
		-I$(JAVA_HOME)/include \
		-I$(JAVA_HOME)/include/linux \
		-Ibuild/jvm/frida-gumjs \
		-DFRIDA_JAVA_TESTS_DATA_DIR=\"$(abspath build)\" \
		-DFRIDA_JAVA_TESTS_CACHE_DIR=\"$(abspath build)/jvm/cache\" \
		runner.c \
		build/jvm/frida-gumjs/libfrida-gumjs.a \
		-Wl,--no-as-needed -lstdc++ -Wl,--as-needed \
		-ldl -lm -lrt -lresolv -pthread \
		-o $@

build/jvm/frida-gumjs/libfrida-gumjs.a:
	@mkdir -p $(@D)
	curl -fsSL https://github.com/frida/frida/releases/download/$(frida_version)/frida-gumjs-devkit-$(frida_version)-linux-$(jvm_arch).tar.xz -o $@.tar.xz.tmp
	tar -xJf $@.tar.xz.tmp -C $(@D)
	rm -f $@.tar.xz.tmp

build/tests-android.dex: build/tests-android.jar
	d8 \
		--output build/tests-android.zip \
		--classpath $(android_jar) \
		--min-api $(android_api_major) \
		$<
	cd build && unzip -o tests-android.zip classes.dex
	mv build/classes.dex $@

build/tests-android.jar: $(android_test_sources) build/junit.jar build/hamcrest.jar
	@rm -rf build/java-android/
	@mkdir -p build/java-android/
	cd build/java-android/ \
		&& jar xf ../junit.jar \
		&& jar xf ../hamcrest.jar
	javac \
		-cp .:build/java-android/:$(android_jar) \
		-bootclasspath $(android_jar) \
		-source 1.8 \
		-target 1.8 \
		-Xlint:deprecation \
		-Xlint:unchecked \
		$(android_test_sources) \
		-d build/java-android/
	jar cf $@ -C build/java-android .

build/tests-jvm.jar: $(jvm_test_sources) build/junit.jar build/hamcrest.jar
	@rm -rf build/java-jvm/
	@mkdir -p build/java-jvm/
	cd build/java-jvm/ \
		&& jar xf ../junit.jar \
		&& jar xf ../hamcrest.jar
	javac \
		-cp .:build/java-jvm/ \
		-source 1.8 \
		-target 1.8 \
		-Xlint:deprecation \
		-Xlint:unchecked \
		$(jvm_test_sources) \
		-d build/java-jvm/
	jar cf $@ -C build/java-jvm .

build/junit.jar:
	@mkdir -p $(@D)
	curl -fsSL https://github.com/junit-team/junit4/releases/download/r4.12/junit-4.12.jar -o $@.tmp
	mv $@.tmp $@

build/hamcrest.jar:
	@mkdir -p $(@D)
	curl -fsSL https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar -o $@.tmp
	mv $@.tmp $@

build/frida-java-bridge.js: node_modules $(bridge_sources)
	npm run build

node_modules: package.json
	npm install

.PHONY: all clean build-android deploy-android run-android watch-android debug-android build-jvm run-jvm watch-jvm debug-jvm
