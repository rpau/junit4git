package org.scalatest.junit

import org.apache.commons.logging.LogFactory
import org.junit.runner.Description
import org.junit.runner.notification.{Failure, RunNotifier}
import org.scalatest._
import org.walkmod.junit4git.junit4.Junit4GitListener

class ScalaGitRunner(suiteClass: java.lang.Class[_ <: Suite]) extends org.junit.runner.Runner {

  var listener:Junit4GitListener = new Junit4GitListener()

  val log = LogFactory.getLog(classOf[ScalaGitRunner])


  def setUpListener(notifier: RunNotifier) = {
    if (listener != null) {
      notifier.addListener(listener);
      removeListener();
    }
  }

  def removeListener(): Unit = {
    listener = null
  }

  private val canInstantiate = Suite.checkForPublicNoArgConstructor(suiteClass)
  require(canInstantiate, "Must pass an org.scalatest.Suite with a public no-arg constructor")

  private val suiteToRun = suiteClass.newInstance

  /**
    * Get a JUnit <code>Description</code> for this ScalaTest <code>Suite</code> of tests.
    *
    * return a <code>Description</code> of this suite of tests
    */
  val getDescription = createDescription(suiteToRun)

  private def createDescription(suite: Suite): Description = {
    val description = Description.createSuiteDescription(suite.getClass)
    // If we don't add the testNames and nested suites in, we get
    // Unrooted Tests show up in Eclipse
    for (name <- suite.testNames) {
      description.addChild(Description.createTestDescription(suite.getClass, name))
    }
    for (nestedSuite <- suite.nestedSuites) {
      description.addChild(createDescription(nestedSuite))
    }
    description
  }

  /**
    * Run this <code>Suite</code> of tests, reporting results to the passed <code>RunNotifier</code>.
    * This class's implementation of this method invokes <code>run</code> on an instance of the
    * <code>suiteClass</code> <code>Class</code> passed to the primary constructor, passing
    * in a <code>Reporter</code> that forwards to the  <code>RunNotifier</code> passed to this
    * method as <code>notifier</code>.
    *
    * @param notifier the JUnit <code>RunNotifier</code> to which to report the results of executing
    * this suite of tests
    */
  def run(notifier: RunNotifier): Unit = {
    setUpListener(notifier)
    notifier.fireTestRunStarted(getDescription)
    try {
      // TODO: What should this Tracker be?
      suiteToRun.run(None, Args(new RunNotifierReporter(notifier),
        Stopper.default, Filter(), ConfigMap.empty, None,
        new Tracker, Set.empty))
    }
    catch {
      case e: Exception =>
        notifier.fireTestFailure(new Failure(getDescription, e))
    }
  }

  /**
    * Returns the number of tests that are expected to run when this ScalaTest <code>Suite</code>
    * is run.
    *
    *  @return the expected number of tests that will run when this suite is run
    */
  override def testCount() = suiteToRun.expectedTestCount(Filter())

}

