package org.canve.util

/*
 * Note:
 * 
 * being a compiler plugin, only core scala or java libraries accessible to core scala,
 * may be used; external libraries will not work even though the compiler plugin itself will compile  
 */
import java.io.{File}
import java.nio.file.{Path, Paths, Files}
import java.nio.charset.StandardCharsets
import java.nio.file.FileAlreadyExistsException

object CanveDataIO {  
  
  val canveRoot = "canve-data"
  
  createDir(canveRoot)
  
  /*
   * write string to file, overwriting if file already exists
   */
  def writeOutputFile(dir: String, fileName: String, fileText: String) {
    createDir(canveRoot + File.separator + dir)
    scala.tools.nsc.io.File(canveRoot + File.separator + dir + File.separator + fileName).writeAll(fileText)
  }

  /*
   * create target folder, if it doesn't already exist
   */
  def createDir(outDir: String) = {
    val outDirObj = Paths.get(outDir)
    try {
      Files.createDirectory(outDirObj)
    } catch { // ignore if directory already exists
      case e: FileAlreadyExistsException =>
      case e: Throwable => throw(e)  
    }
  }
  
  /*
   * get sub-directories
   */
  def getSubDirectories(dir: String): Array[File] = {
    new File(dir).listFiles.filter(_.isDirectory())
  }
  
  def clearAll = {
    clearRecursive(new File(canveRoot))
  }
  
  private def clearRecursive(obj: File): Unit = {
    if (obj.isDirectory) obj.listFiles.foreach(clearRecursive)
    if (obj.toString != canveRoot.toString) // avoids both deleting it and relying on its existence 
      safeDelete(obj)
  }
  
  private def safeDelete(obj: File) = {
    println(obj.toString)
    if (obj.toString.startsWith(canveRoot)) 
      obj.delete
    else 
      throw new Exception(s"safe delete captured an invalid delete attempt (${obj.toString})")
  }

} 
