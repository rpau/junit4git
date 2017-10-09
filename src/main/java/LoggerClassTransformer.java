import com.google.gson.JsonArray;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class LoggerClassTransformer implements ClassFileTransformer {

    private JsonArray array;

    public void createContext() {
        array = new JsonArray();
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {

        if (array != null) {
            array.add(className);
        }

        return classfileBuffer;
    }

    public JsonArray destroyContext() {
        return array;
    }
}
