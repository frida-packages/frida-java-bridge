all:
	@echo "Nothing to build. To run the test-suite: make check-android or make check-jvm"

check-android: node_modules
	$(MAKE) -C test deploy-android
	@echo ""
	@echo "[*] Running the Android test suite with optimizations disabled (interpreter mode)."
	@echo ""
	@$(MAKE) -C test run-android
	@echo ""
	@echo "[*] Running the Android test suite with optimizations enabled."
	@echo ""
	@$(MAKE) -C test RUNNER_ARGS="--enable-optimizations" run-android

check-android-gdb: node_modules
	$(MAKE) -C test deploy-android
	$(MAKE) -C test debug-android

develop-android: node_modules
	$(MAKE) -C test deploy-android
	$(MAKE) -C test watch-android

check-jvm: node_modules
	$(MAKE) -C test build-jvm
	@echo ""
	@echo "[*] Running the JVM test suite with optimizations disabled (interpreter mode)."
	@echo ""
	@$(MAKE) -C test run-jvm
	@echo ""
	@echo "[*] Running the JVM test suite with optimizations enabled."
	@echo ""
	@$(MAKE) -C test RUNNER_ARGS="--enable-optimizations" run-jvm

check-jvm-gdb: node_modules
	$(MAKE) -C test build-jvm
	$(MAKE) -C test debug-jvm

develop-jvm: node_modules
	$(MAKE) -C test build-jvm
	$(MAKE) -C test watch-jvm

node_modules: package.json
	npm install

clean:
	rm -rf node_modules
	$(MAKE) -C test clean

.PHONY: all check-android check-android-gdb develop-android check-jvm check-jvm-gdb develop-jvm clean
