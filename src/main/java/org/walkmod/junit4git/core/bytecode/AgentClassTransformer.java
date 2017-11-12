package org.walkmod.junit4git.core.bytecode;

import org.walkmod.junit4git.javassist.JavassistUtils;

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
            String normalizedName = normalizeName(className);
            try {
                return instrumentClass(normalizedName, classfileBuffer);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        return classfileBuffer;
    }

    public byte[] instrumentClass(String name, byte[] classfileBuffer) {
        try {
            return new JavassistUtils().instrumentClass(name,
                    AgentClassTransformer.class.getName()
                            + ".add(\"" + name + "\");");
        } catch (Throwable e) {
            e.printStackTrace();
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

    public static Set<String> getReferencedClasses() {
        return referencedClasses;
    }
}
