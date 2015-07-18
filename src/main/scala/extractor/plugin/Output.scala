package extractor.plugin

import myUtil.fileUtil._

object Output {
  def write = {
    println("Writing extracted type relations and call graph....")
    myUtil.fileUtil.writeOutputFile("nodes", Nodes.list.map(_._2.toString).mkString("\n"))
    myUtil.fileUtil.writeOutputFile("edges", Edges.list.mkString("\n"))
  }
}