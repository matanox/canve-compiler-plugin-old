package extractor.plugin

object Nodes {
  
  var list: Map[Int, Node] = Map()

  def apply(id: Int, name: String, kind: String): Boolean = {
    if (list.contains(id)) false else 
    if (name.startsWith("apply$default$")) false else // skip synthetic methods created by scala for 
                                                      // default argument values. This is only one case
                                                      // of a bunch where scala creates synthetic entities,
                                                      // it is not necessarily a good idea to skip them,
                                                      // as many of them can contain children that contain
                                                      // stuff that *should* be part of the result graphs
    {
      list += (id -> Node(id, name, kind))
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

                      
