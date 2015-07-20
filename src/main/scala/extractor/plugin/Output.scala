package extractor.plugin

import myUtil.fileUtil._

object Output {
  
  def quote(int: Int) = "\"" + int.toString() + "\""
  
  def write = {
    println("Writing extracted type relations and call graph....")
    myUtil.fileUtil.writeOutputFile("nodes", Nodes.list.map(_._2.toString).mkString("\n"))
    myUtil.fileUtil.writeOutputFile("edges", Edges.list.mkString("\n"))
    
    // see https://github.com/cpettitt/graphlib/wiki for Dagre graph specification
    myUtil.fileUtil.writeOutputFile("dagre", 
        Nodes.list.map(node => 
          "g.setNode(" + quote(node._2.id) + ", " +
                     "{ " + 
                       "name: " + "\"" + node._2.name + "\"" +
                       ", " +
                       "kind: " + "\"" + node._2.kind + "\"" +
                       ", " + "width:10" +
                       ", " + "height:10" +
                     " });")
          .mkString("\n") +
          
          "\n" +
           
        Edges.list.map(edge => 
          "g.setEdge(" + quote(edge.id1) + ", " + 
                      quote(edge.id2) + ", " +
                     "{ " +
                       "edgeKind: " + "\"" + edge.edgeKind + "\"" + 
                     " });")
          .mkString("\n"))
  }
}