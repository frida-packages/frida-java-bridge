package re.frida;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import java.io.IOException;

public class ClassInitializerTest {
    public static boolean instanceTargetInitialized = false;
    public static boolean staticTargetInitialized = false;
    public static boolean combinedTargetInitialized = false;
    public static boolean staticFieldTargetInitialized = false;
    public static boolean revertableTargetInitialized = false;

    @Test
    public void classInitializerCanBeHooked() {
        loadScript("var Lazy = Java.use('re.frida.HookablyInitialized');" +
                "Lazy.$clinit.implementation = function () {" +
                    "send('before');" +
                    "this.$clinit();" +
                    "send('after');" +
                "};");

        assertEquals("initialized", HookablyInitialized.greeting);
        assertEquals("before", script.getNextMessage());
        assertEquals("after", script.getNextMessage());
    }

    @Test
    public void classInitializerCanBeReplaced() {
        loadScript("var Lazy = Java.use('re.frida.ReplaceablyInitialized');" +
                "Lazy.$clinit.implementation = function () {" +
                    "send('skipped');" +
                "};");

        assertEquals(null, ReplaceablyInitialized.greeting);
        assertEquals("skipped", script.getNextMessage());
    }

    @Test
    public void classInitializerIsEnumerable() {
        loadScript("Java.use('re.frida.EnumerablyInitialized');" +
                "var names = [];" +
                "Java.enumerateMethods('re.frida.EnumerablyInitialized!*').forEach(function (group) {" +
                    "group.classes.forEach(function (klass) {" +
                        "names = names.concat(klass.methods);" +
                    "});" +
                "});" +
                "send(names.indexOf('$clinit') !== -1 ? 'found' : names.join(','));");

        assertEquals("found", script.getNextMessage());
    }

    @Test
    public void classWithoutClassInitializerThrows() {
        loadScript("var Bare = Java.use('re.frida.NeverInitialized');" +
                "try {" +
                    "Bare.$clinit;" +
                    "send('no error');" +
                "} catch (e) {" +
                    "send(e.message);" +
                "}");

        assertEquals("re.frida.NeverInitialized has no class initializer",
                script.getNextMessage());
    }

    @Test
    public void hookingInstanceMethodDoesNotInitializeClass() {
        loadScript("var Target = Java.use('re.frida.LazyInstanceHookTarget');" +
                "Target.$init.implementation = function () {" +
                    "send('init');" +
                    "this.$init();" +
                "};" +
                "Target.greet.implementation = function () {" +
                    "send('greet');" +
                    "return this.greet() + '!';" +
                "};");

        assertFalse(instanceTargetInitialized);

        LazyInstanceHookTarget obj = new LazyInstanceHookTarget();
        assertTrue(instanceTargetInitialized);
        assertEquals("hello!", obj.greet());
        assertEquals("init", script.getNextMessage());
        assertEquals("greet", script.getNextMessage());
    }

    @Test
    public void hookingStaticMethodDoesNotInitializeClass() {
        loadScript("var Test = Java.use('re.frida.ClassInitializerTest');" +
                "var Target = Java.use('re.frida.LazyStaticHookTarget');" +
                "Target.format.implementation = function (s) {" +
                    "send(Test.staticTargetInitialized.value ? 'hook-after-clinit' : 'hook-before-clinit');" +
                    "return this.format(s) + '!';" +
                "};");

        assertFalse(staticTargetInitialized);

        assertEquals("ready:ok!", LazyStaticHookTarget.format("ok"));
        assertTrue(staticTargetInitialized);
        assertEquals("hook-after-clinit", script.getNextMessage());

        assertEquals("ready:again!", LazyStaticHookTarget.format("again"));
        assertEquals("hook-after-clinit", script.getNextMessage());
    }

    @Test
    public void classInitializerAndStaticMethodCanBothBeHooked() {
        loadScript("var Target = Java.use('re.frida.CombinedHookTarget');" +
                "Target.$clinit.implementation = function () {" +
                    "send('clinit-before');" +
                    "this.$clinit();" +
                    "send('clinit-after');" +
                "};" +
                "Target.getTag.implementation = function () {" +
                    "send('static-hook');" +
                    "return this.getTag() + ':hooked';" +
                "};");

        assertFalse(combinedTargetInitialized);

        assertEquals("v1:hooked", CombinedHookTarget.getTag());
        assertTrue(combinedTargetInitialized);
        assertEquals("clinit-before", script.getNextMessage());
        assertEquals("clinit-after", script.getNextMessage());
        assertEquals("static-hook", script.getNextMessage());

        assertEquals("v1:hooked", CombinedHookTarget.getTag());
        assertEquals("static-hook", script.getNextMessage());
    }

    @Test
    public void invokingStaticMethodAndConstructorFromJsInitializesClass() {
        loadScript("var StaticTarget = Java.use('re.frida.JsStaticInvokeTarget');" +
                "send(StaticTarget.getLabel());" +
                "var CtorTarget = Java.use('re.frida.JsCtorInvokeTarget');" +
                "send(CtorTarget.$new().getBase());");

        assertEquals("static-ok", script.getNextMessage());
        assertEquals("ctor-ok", script.getNextMessage());
    }

    @Test
    public void accessingStaticFieldFromJsInitializesClass() {
        loadScript("var Test = Java.use('re.frida.ClassInitializerTest');" +
                "var Target = Java.use('re.frida.JsStaticFieldTarget');" +
                "var field = Target.greeting;" +
                "send(field.fieldType + ':' + Test.staticFieldTargetInitialized.value);" +
                "send(field.value);" +
                "send('' + Test.staticFieldTargetInitialized.value);");

        assertEquals("1:false", script.getNextMessage());
        assertEquals("field-ok", script.getNextMessage());
        assertEquals("true", script.getNextMessage());
    }

    @Test
    public void staticMethodHookFiresWhenCalledFromClassInitializer() {
        loadScript("var Target = Java.use('re.frida.StaticFromClinitTarget');" +
                "Target.compute.implementation = function () {" +
                    "send('in-clinit');" +
                    "return this.compute() + 1;" +
                "};");

        assertEquals(42, StaticFromClinitTarget.value);
        assertEquals("in-clinit", script.getNextMessage());

        assertEquals(42, StaticFromClinitTarget.compute());
        assertEquals("in-clinit", script.getNextMessage());
    }

    @Test
    public void revertedPreInitStaticMethodHookRunsOriginal() throws IOException {
        loadScript("var Target = Java.use('re.frida.RevertableStaticHookTarget');" +
                "Target.getValue.implementation = function () {" +
                    "return this.getValue() + ':hooked';" +
                "};");

        assertFalse(revertableTargetInitialized);

        assertEquals("v1:hooked", RevertableStaticHookTarget.getValue());
        assertTrue(revertableTargetInitialized);

        unloadScript();
        assertEquals("v1", RevertableStaticHookTarget.getValue());
    }

    private Script script = null;

    private void loadScript(String code) {
        Script script = new Script(TestRunner.bundleWith(
                "(function (Java) {" +
                "Java.perform(function () {" +
                code +
                "});" +
                "})(LocalJava);"));
        this.script = script;
    }

    private void unloadScript() throws IOException {
        if (script != null) {
            script.close();
            script = null;
        }
    }

    @After
    public void tearDown() throws IOException {
        unloadScript();
    }
}

class HookablyInitialized {
    public static String greeting = "initialized";
}

class ReplaceablyInitialized {
    public static String greeting = "initialized";
}

class EnumerablyInitialized {
    public static String greeting = "initialized";
}

class NeverInitialized {
}

class LazyInstanceHookTarget {
    static {
        ClassInitializerTest.instanceTargetInitialized = true;
    }

    public String greet() {
        return "hello";
    }
}

class LazyStaticHookTarget {
    private static String prefix;

    static {
        prefix = "ready:";
        ClassInitializerTest.staticTargetInitialized = true;
    }

    public static String format(String value) {
        return prefix + value;
    }
}

class CombinedHookTarget {
    private static String tag;

    static {
        tag = "v1";
        ClassInitializerTest.combinedTargetInitialized = true;
    }

    public static String getTag() {
        return tag;
    }
}

class JsStaticInvokeTarget {
    private static String label;

    static {
        label = "static-ok";
    }

    public static String getLabel() {
        return label;
    }
}

class JsCtorInvokeTarget {
    private static String base;

    static {
        base = "ctor-ok";
    }

    public String getBase() {
        return base;
    }
}

class JsStaticFieldTarget {
    public static String greeting;

    static {
        greeting = "field-ok";
        ClassInitializerTest.staticFieldTargetInitialized = true;
    }
}

class StaticFromClinitTarget {
    public static int value;

    static {
        value = compute();
    }

    public static int compute() {
        return 41;
    }
}

class RevertableStaticHookTarget {
    private static String value;

    static {
        value = "v1";
        ClassInitializerTest.revertableTargetInitialized = true;
    }

    public static String getValue() {
        return value;
    }
}
