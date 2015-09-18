import extractor.plugin.{TraversalExtraction, Graph}
import scala.tools.nsc.Global
import utest._
import utest.ExecutionContext.RunNow
import compilerPluginUnitTest.InjectedCompiler

class TraversalExtractionTester extends compilerPluginUnitTest.InjectablePluginComponent {
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
  val compiler = InjectedCompiler.default(new InstantiationCycleTester)
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

