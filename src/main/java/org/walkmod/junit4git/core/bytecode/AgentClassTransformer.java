package org.walkmod.junit4git.core.bytecode;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public class AgentClassTransformer implements ClassFileTransformer {

    private static Set<String> referencedClasses = new LinkedHashSet<>();

    private static String CLASS_EXTENSION = ".class";

    public static void cleanUp() {
        referencedClasses = new LinkedHashSet<>();
    }

    /**
     * Stores a new reference. It is called by the application constructors.
     * @param name full class name to store
     */
    public static void add(String name) {
        referencedClasses.add(name);
    }

    protected boolean belongsToAJarFile(ProtectionDomain protectionDomain) {
        return Optional.of(protectionDomain.getCodeSource())
                .map(source -> source.getLocation().getPath().endsWith(".jar"))
                .orElse(true);
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
        String aux = className.replaceAll("/", "\\.");
        if (aux.endsWith(CLASS_EXTENSION)) {
            aux = aux.substring(0, aux.length() - CLASS_EXTENSION.length());
        }
        return aux;
    }

    public byte[] instrumentConstructors(String className, byte[] classfileBuffer) {
        ClassPool pool = ClassPool.getDefault();
        try {
            CtClass clazz = pool.get(className);
            clazz.defrost();
            for(CtConstructor ctConstructor: clazz.getConstructors()) {
                try {
                    //here we are calling the static add method of this class
                    ctConstructor.insertAfter(AgentClassTransformer.class.getName()
                            + ".add(\"" + clazz.getName() +"\");");

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

    public static Set<String> getReferencedClasses() {
        return referencedClasses;
    }
}
