package org.scalatest.junit

import java.lang.instrument.Instrumentation
import java.util

import org.junit.Test
import org.scalatest.FunSuite
import org.scalatest.mockito.MockitoSugar
import org.walkmod.junit4git.core.ignorers.TestIgnorer
import org.walkmod.junit4git.core.reports.{AbstractTestReportStorage, TestMethodReport}
import org.mockito.Mockito.when
import org.mockito.Mockito.verify
import org.mockito.Matchers.any
import org.walkmod.junit4git.javassist.JavassistUtils

class TestIgnorerTest extends MockitoSugar {

  @Test
  def testReplacesTestsByIgnores(): Unit = {
    val testReportStorage = mock[AbstractTestReportStorage]
    val javassist = mock[JavassistUtils]

    val testIgnorer = new TestIgnorer(".", testReportStorage, javassist) {

      protected override def getTestsToIgnore(report: Array[TestMethodReport]): util.Set[TestMethodReport] = {
        val result = new util.HashSet[TestMethodReport]()
        result.add(report(0))
        result
      }
    }

    val instrumentation = mock[Instrumentation]

    when(testReportStorage.getBaseReport).thenReturn(
      Array(new TestMethodReport(classOf[MyTestClass].getName, "hello junit4git", new util.HashSet())))

    testIgnorer.ignoreTests(instrumentation)

    verify(javassist).replaceMethodCallOnConstructors(any(), any(), any())
  }

}

class MyTestClass extends FunSuite {

  test("hello junit4git") {

  }
}
