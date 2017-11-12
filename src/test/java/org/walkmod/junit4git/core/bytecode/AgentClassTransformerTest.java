package org.walkmod.junit4git.core.bytecode;

import org.junit.Assert;
import org.junit.Test;
import org.walkmod.junit4git.core.Dummy;

import java.util.Arrays;
import java.util.LinkedHashSet;

public class AgentClassTransformerTest {

  private static class TransformingClassLoader extends ClassLoader {

    private final String className;

    final AgentClassTransformer transformer = new AgentClassTransformer();

    public TransformingClassLoader(String className) {
      super();
      this.className = className;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
      if (name.contains(className)) {
        byte[] byteBuffer = transformer.instrumentClass(name, new byte[0]);
        return defineClass(name, byteBuffer, 0, byteBuffer.length);
      }
      return super.loadClass(name);
    }
  }

  @Test
  public void whenAClassIsLoadedThenConstructorsBecomeInstrumented() throws Exception {
    AgentClassTransformer.cleanUp();
    ClassLoader classLoader = new TransformingClassLoader("Dummy");
    Class<?> clazz = classLoader.loadClass(Dummy.class.getName());
    clazz.newInstance();
    Assert.assertEquals(new LinkedHashSet<String>(Arrays.asList(Dummy.class.getName())),
            AgentClassTransformer.getReferencedClasses());
  }
}
