package extractor.plugin
import tools.nsc.Global

object Nodes {
  
  var list: Map[Int, Node] = Map()

  def apply(global: Global)(s: global.Symbol): Boolean = {
    if (list.contains(s.id)) false else 
    {
      list += (s.id -> Node(s.id, s.nameString, s.kindString))
      SourceExtract(global)(s)
      true
    }
  }
  
}

object Edges {
  
  var list: List[Edge] = List()
  
  def apply(id1: Int, edgeKind: String, id2: Int): Unit = 
    list = Edge(id1, edgeKind, id2) :: list
}

case class Edge(id1: Int,
                edgeKind: String,
                id2: Int) 
                
case class Node(id: Int,
                name: String,
                kind: String)  
