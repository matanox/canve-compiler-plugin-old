/*
 * 
 * this code just demonstrates that an AST cannot
 * be used to get the original source code - things like 
 * closing brackets are not part of the AST, and the 
 * only thing that is guaranteed, is that each symbol
 * will have its original source line available.
 * 
 * Whatever's not a symbol is simply not included - 
 * the AST trees and sub-trees just end where they 
 * do - the delimiting characters are not required
 * to explicitly exist in them to represent the program.
 *   
 */

package extractor.plugin

import scala.collection.{SortedSet, mutable}
import scala.tools.nsc.{Global, Phase}
import tools.nsc.plugins.PluginComponent

class SourcePluginPhase(val global: Global)
                  extends PluginComponent
                  { t =>

  import global._

  val runsAfter = List("typer")

  override val runsRightAfter = Some("typer")
  
  val phaseName = "trial"

  def units = global.currentRun
                    .units
                    .toSeq
                    .sortBy(_.source.content.mkString.hashCode())

  override def newPhase(prev: Phase): Phase = new Phase(prev) {
    override def run() {
      println
      println("**** running trial")
      units.foreach { unit =>
        println("* examining " + unit.source.path)
        SourceExtraction(t.global)(unit)   
      }
    }

    def name: String = "trial"
  }

  object SourceExtraction {
  
    def apply(global: Global)(unit: global.CompilationUnit) = {
      import global._
    
      class ExtractSourceSpan extends Traverser {
        override def traverse(tree: Tree): Unit = {

          println("line " + tree.pos.line + ": " + tree.pos.lineContent + "  | sybmol: " + tree.symbol)
          
          super.traverse(tree)
        }
      }
  
      val traverser = new ExtractSourceSpan
      traverser.traverse(unit.body)
      }
    } 
  }