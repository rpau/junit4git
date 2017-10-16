package rpau.smartesting.core;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;

public class LoggerClassTransformerTest {

    private static class TransformingClassLoader extends ClassLoader {

        private final String className;

        final LoggerClassTransformer transformer = new LoggerClassTransformer();

        public TransformingClassLoader(String className) {
            super();
            this.className = className;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.contains(className)) {
                byte[] byteBuffer = transformer.instrumentConstructors(name, new byte[0]);
                return defineClass(name, byteBuffer, 0, byteBuffer.length);
            }
            return super.loadClass(name);
        }
    }

    @Test
    public void whenAClassIsLoadedThenConstructorsBecomeInstrumented() throws Exception {
        LoggerClassTransformer.destroyContext();
        ClassLoader classLoader = new TransformingClassLoader("Dummy");
        Class<?> clazz = classLoader.loadClass(Dummy.class.getName());
        clazz.newInstance();
        Assert.assertEquals(new LinkedHashSet<String>(Arrays.asList(Dummy.class.getName())),
                LoggerClassTransformer.destroyContext());
    }
}
