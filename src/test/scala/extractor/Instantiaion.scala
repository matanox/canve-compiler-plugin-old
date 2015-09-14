import extractor.plugin.{TraversalExtraction, Graph}
import scala.tools.nsc.Global
import utest._
import utest.ExecutionContext.RunNow
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

class TraversalExtractionTester extends scoverage.InjectablePluginComponent {
  def apply(global: Global)(body: global.Tree) = {
    TraversalExtraction(global)(body)    
  }  
}

case class InstantiationCycleTester() extends TraversalExtractionTester {
  override def apply(global: Global)(body: global.Tree) = {
    val graph: Graph = TraversalExtraction(global)(body)    
  }  
}

object MyTestSuite extends TestSuite {
  val compiler = ScoverageCompiler.default(new InstantiationCycleTester)
  assert(!compiler.reporter.hasErrors)
  
  val tests = TestSuite {
    'instantiation { 
      
      'case1 {  
        compiler.compileCodeSnippet("""
          case class Bar(a:Int) 
          object Foo {
            def get(g: Int) = {
              new Bar(g)
            }
          }
        """)
        assert(!compiler.reporter.hasErrors)
      }
      
      'case2 {  
        compiler.compileCodeSnippet("""
          case class Bar(a:Int) 
          object Foo {
            def get(g: Int) = {
              Bar(g)
            }
          }
          """)
        assert(!compiler.reporter.hasErrors)
      }

      'case3 {  
        compiler.compileCodeSnippet("""
          class Bar(a:Int) 
          object Foo {
            def get(g: Int) = {
              new Bar(g)
            }
          }
          """)
        assert(!compiler.reporter.hasErrors)
      }
    }
  }
  
  val results = tests.run()
  
  //println(results.toSeq.length) // 4
  //println(results.leaves.length) // 3
  //println(results.leaves.count(_.value.isFailure)) // 2
  //println(results.leaves.count(_.value.isSuccess)) // 1
}

