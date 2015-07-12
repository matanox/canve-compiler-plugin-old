package extractor.plugin

object Nodes {
  
  var list: Map[Int, Node] = Map()
  
  def apply(id: Int, name: String, kind: String): Unit = 
    if (!list.contains(id)) list += (id -> Node(id, name, kind)) 
  
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

                