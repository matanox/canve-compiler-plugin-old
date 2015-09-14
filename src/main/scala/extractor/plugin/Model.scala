package extractor.plugin
import tools.nsc.Global

object Nodes {
  
  var list: Map[Int, Node] = Map()

  def apply(global: Global)(s: global.Symbol): Node = {
    
    if (list.contains(s.id))
      list.get(s.id).get
    else
    {
      val newNode = s.sourceFile match {
        case null => // no source file included in this project for this entity 
          Node(s.id, s.nameString, s.kindString, !(s.isSynthetic), None, None)
        case _    => 
          Node(s.id, s.nameString, s.kindString, !(s.isSynthetic), SourceExtract(global)(s), Some(s.sourceFile.toString))
      }
      
      list += (s.id -> newNode)
      newNode
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
                kind: String,
                notSynthetic: Boolean,
                source: Option[String],
                fileName: Option[String]) {
  var ownersTraversed = false
}  

case class Graph(nodes: List[Node], edges: List[Edge])
