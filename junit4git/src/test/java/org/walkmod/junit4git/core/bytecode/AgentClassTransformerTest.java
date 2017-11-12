package org.walkmod.junit4git.core.bytecode;

import org.junit.Assert;
import org.junit.Test;
import org.walkmod.junit4git.core.Dummy;
import org.walkmod.junit4git.javassist.JavassistUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

  @Test
  public void when_transform_is_called_then_instruments_a_class() throws Exception {
    JavassistUtils javassist = mock(JavassistUtils.class);

    AgentClassTransformer transformer = new AgentClassTransformer(javassist);
    transformer.transform(new TransformingClassLoader("org.walkmod.junit4git.core.Dummy"),
            "org.walkmod.junit4git.core.Dummy",
            Dummy.class,
            this.getClass().getProtectionDomain(), new byte[0]);

    verify(javassist).instrumentClass("org.walkmod.junit4git.core.Dummy",
            AgentClassTransformer.class.getName() + ".add(\"" +  Dummy.class.getName() + "\");");
  }
}
