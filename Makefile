all:
	@echo "Nothing to build. To run the test-suite: make check"

check: node_modules
	$(MAKE) -C test deploy
	@echo ""
	@echo "[*] Running the test suite with optimizations disabled (interpreter mode)."
	@echo ""
	@$(MAKE) -C test run
	@echo ""
	@echo "[*] Running the test suite with optimizations enabled."
	@echo ""
	@$(MAKE) -C test RUNNER_ARGS="--enable-optimizations" run

check-run: node_modules
	$(MAKE) -C test deploy
	$(MAKE) -C test run

check-gdb: node_modules
	$(MAKE) -C test deploy
	$(MAKE) -C test debug

develop: node_modules
	$(MAKE) -C test deploy
	$(MAKE) -C test watch

node_modules: package.json
	npm install

clean:
	rm -rf node_modules
	$(MAKE) -C test clean

.PHONY: all check check-run check-gdb develop clean
