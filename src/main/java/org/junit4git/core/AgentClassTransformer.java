package org.junit4git.core;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.LinkedHashSet;
import java.util.Set;

public class AgentClassTransformer implements ClassFileTransformer {

    private static Set<String> referencedClasses = new LinkedHashSet<>();

    public static void createContext() {
        referencedClasses = new LinkedHashSet<>();
    }

    public static void add(String name) {
        referencedClasses.add(name);
    }

    protected boolean belongsToAJarFile(ProtectionDomain protectionDomain) {
        return protectionDomain.getCodeSource().getLocation().getPath().endsWith(".jar");
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {

        if (className != null && !belongsToAJarFile(protectionDomain)) {
            return instrumentConstructors(normalizeName(className), classfileBuffer);
        }

        return classfileBuffer;
    }

    private String normalizeName(String className) {
        return className.replaceAll("/", "\\.");
    }

    public byte[] instrumentConstructors(String className, byte[] classfileBuffer) {
        ClassPool pool = ClassPool.getDefault();
        try {
            CtClass clazz = pool.get(className);
            clazz.defrost();
            for(CtConstructor ctConstructor: clazz.getConstructors()) {
                try {
                    ctConstructor.insertAfter(AgentClassTransformer.class.getName()
                            + ".add(\"" + className +"\");");

                } catch (CannotCompileException e) {
                    e.printStackTrace();
                }
            }
            clazz.defrost();
            return clazz.toBytecode();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return classfileBuffer;
    }

    public static Set<String> destroyContext() {
        return referencedClasses;
    }
}
