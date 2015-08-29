package extractor.plugin
import scala.collection.{SortedSet, mutable}
import scala.tools.nsc.{Global, Phase}
import tools.nsc.plugins.PluginComponent

class PluginPhase(val global: Global)
                  extends PluginComponent
                  { t =>

  import global._

  val runsAfter = List("typer")

  override val runsRightAfter = Some("typer")
  
  val phaseName = "extractor"

  def units = global.currentRun
                    .units
                    .toSeq
                    .sortBy(_.source.content.mkString.hashCode())

  override def newPhase(prev: Phase): Phase = new Phase(prev) {
    override def run() {
      
      println("\n** running as part of project compilation ** \n")
      
      println(t.global.currentSettings)
      
      units.foreach { unit =>
        if (unit.source.path.endsWith(".scala")) {
          println("\n* examining " + unit.source.path + "* \n")
          TraversalExtraction(t.global)(unit)  
        } else
            println("\n* skipping non-scala source file: " + unit.source.path + "\n")
      }
    }

    def name: String = "extractor"
  }

}