package myUtil

object Logging {
  def logEdge(nodeA: Int, relation: String, nodeB: Int) = {    
    println(nodeA + " " + relation + " " + nodeB)
  }
}