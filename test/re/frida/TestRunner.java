package re.frida;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.runner.JUnitCore;

public class TestRunner {
    public static String fridaJavaBundle;
    public static long classLoaderPointer;

    private static String dataDir;
    private static String cacheDir;

    public static void main(String[] args, String dataDir, String cacheDir,
            long classLoaderPointer) {
        TestRunner.dataDir = dataDir;
        TestRunner.cacheDir = cacheDir;
        TestRunner.classLoaderPointer = classLoaderPointer;

        TestRunner.fridaJavaBundle = slurp("frida-java-bridge.js");

        registerClassLoader(TestRunner.class.getClassLoader());

        String[] classes = (args.length > 1)
                ? Arrays.copyOfRange(args, 1, args.length)
                : new String[] {
                    "re.frida.ClassRegistryTest",
                    "re.frida.MethodTest",
                    "re.frida.ClassCreationTest",
                    "re.frida.ClassInitializerTest",
                    "re.frida.PerformTest"
                };
        JUnitCore.main(classes);
    }

    public static String getCacheDir() {
        return dataDir;
    }

    public static String getCodeCacheDir() {
        return cacheDir;
    }

    public static String bundleWith(String code) {
        int size = code.getBytes(StandardCharsets.UTF_8).length;
        return fridaJavaBundle +
                "\n\u2704\n" +
                size + " /test.js" +
                "\n\u2704\n" +
                code;
    }

    private static native void registerClassLoader(ClassLoader loader);

    public static String slurp(String name) {
        File file = new File(dataDir, name);
        try {
            DataInputStream input = new DataInputStream(new FileInputStream(file));
            try {
                byte[] data = new byte[(int) file.length()];
                input.readFully(data);
                return new String(data, "UTF-8");
            } finally {
                input.close();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
