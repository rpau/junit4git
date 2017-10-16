import com.google.gson.JsonArray;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class LoggerClassTransformer implements ClassFileTransformer {

    private static Set<String> array  = new LinkedHashSet<>();

    public static void createContext() {
        array = new LinkedHashSet<>();
    }

    public static void add(String name) {
        array.add(name);
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {

        if (array != null && !protectionDomain.getCodeSource().getLocation().getPath().endsWith(".jar")) {
            add(className);
            return instrumentConstructors(className, classfileBuffer);
        }

        return classfileBuffer;
    }

    private byte[] instrumentConstructors(String className, byte[] classfileBuffer) {
        ClassPool pool = ClassPool.getDefault();
        try {
            CtClass clazz = pool.get(className);
            Arrays.stream(clazz.getConstructors()).forEach(ctConstructor -> {
                try {
                    ctConstructor.insertAfter(LoggerClassTransformer.class.getName()
                            + ".add(\"" + className +"\");");

                } catch (CannotCompileException e) {
                    e.printStackTrace();
                }
            });
            return clazz.toBytecode();
        } catch (Throwable e) {
        }
        return classfileBuffer;
    }

    public static JsonArray destroyContext() {
        JsonArray res = new JsonArray();
        array.stream().forEach(value -> res.add(value));
        return res;
    }
}
