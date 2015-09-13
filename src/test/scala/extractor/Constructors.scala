import extractor.plugin.TraversalExtraction
import utest._
import utest.ExecutionContext.RunNow
import extractor.plugin
import scoverage.ScoverageCompiler

/*
 * demonstrates that handling a toolbox typed tree
 * does not allow access to symbol members which
 * can be accessed in a compiler plugin.
 * Or just my closest shot at it.
 */
object reflectionToolBoxTypeCheckExperiment {
  
  import scala.reflect.runtime.universe._
  import scala.tools.reflect.ToolBox
  
  val tree = reify { final class C { def x = 2 } }.tree
  val tb = runtimeMirror(getClass.getClassLoader).mkToolBox()
  val ttree = tb.typeCheck(tree) // deprecated method is the only way to cross-work on 2.10
  
  // this won't work:
  //println(ttree.symbol.id)                      // undefined method  
}
  
object MyTestSuite extends TestSuite{
  val compiler = ScoverageCompiler.default(TraversalExtraction)
  assert(!compiler.reporter.hasErrors)
  
  val tests = TestSuite{
    'constructors {
      val a: ScoverageCompiler = compiler.compileCodeSnippet("""class C { println(3) }""")
      
    }
  }
  
  val results = tests.run()
  
  //println(results.toSeq.length) // 4
  //println(results.leaves.length) // 3
  //println(results.leaves.count(_.value.isFailure)) // 2
  //println(results.leaves.count(_.value.isSuccess)) // 1
}