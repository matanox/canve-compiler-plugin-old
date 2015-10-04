package extractor

import org.canve.simpleGraph._
import org.canve.simpleGraph.algo.impl._
import extractor.plugin.{Node, Edge} 

package object test {
  type NodeGraph = SimpleGraph[Int, Node, Edge]
}