package extractor.plugin
import tools.nsc.Global
import scala.collection.SortedSet

class RuntimePlugin(global: Global) extends TestPlugin(global)
class TestPlugin(val global: Global) extends tools.nsc.plugins.Plugin {

  val name = "Extractor"
  val description = "extracts type relationships and call graph"

  val components = List[tools.nsc.plugins.PluginComponent](
    new PluginPhase(this.global)
  )
}
