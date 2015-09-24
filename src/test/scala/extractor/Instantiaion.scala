import extractor.plugin.{TraversalExtraction, Graph, Node, Edge}
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
  import org.canve.simpleGraph._
  override def apply(global: Global)(body: global.Tree) = {
    val graph: Graph = TraversalExtraction(global)(body)    
    val simpleGraph = new SimpleGraph[Int, Node, Edge]
    
  }  
}

object MyTestSuite extends TestSuite {
  val compiler = InjectedCompiler.default(new InstantiationCycleTester)
  assert(!compiler.reporter.hasErrors)
  
  val tests = TestSuite {
    "instantiation is reasonably captured" - { 
      
      "case class, with new keyword" - {  
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
      
      "case class, without new keyword" - {  
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

      "non-case class" - {  
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
  
  val results = tests.run().toSeq.foreach(println)

  //println(results.toSeq.length) // 4
  //println(results.leaves.length) // 3
  //println(results.leaves.count(_.value.isFailure)) // 2
  //println(results.leaves.count(_.value.isSuccess)) // 1
}

