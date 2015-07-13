package extractor.plugin

import myUtil.fileUtil._

object Output {
  def write = {
    println("Writing extracted type relations and call graph....")
    myUtil.fileUtil.writeOutputFile("out", Nodes.list.map(_._1.toString).mkString(","))
  }
}