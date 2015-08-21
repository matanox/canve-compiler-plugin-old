package extractor.plugin
import tools.nsc.Global

object Nodes {
  
  var list: Map[Int, Node] = Map()

  def apply(global: Global)(s: global.Symbol): Node = {
    
    if (list.contains(s.id))
      list.get(s.id).get
    else
    {
      val sourceFile = s.sourceFile match {
        case null => "no source file included in this project for this entity"
        case _    => s.sourceFile.toString
      }
      
      val newNode = Node(s.id, s.nameString, s.kindString, SourceExtract(global)(s), sourceFile)
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
                source: List[String],
                fileName: String) {
  var ownersTraversed = false
}  
