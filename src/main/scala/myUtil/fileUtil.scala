package myUtil

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
import scala.tools.nsc.io.Directory

object fileUtil {  
  
  val outDir = "out"
  
  createDir(outDir)
  
  //
  // write string to file, overwriting if file already exists
  //
  def writeOutputFile(fileName: String, fileText: String) {
    scala.tools.nsc.io.File(outDir + "/" + fileName).writeAll(fileText)
    //Files.write(Paths.get(outDir + "/" + fileName), fileText.getBytes(StandardCharsets.UTF_8))
  }
  
  //
  // create target folder, if it doesn't already exist
  //
  def createDir(outDir: String) = {
    val outDirObj = Paths.get(outDir)
    try {
      Files.createDirectory(outDirObj)
    } catch { // ignore if directory already exists
      case e: FileAlreadyExistsException =>
      case e: Throwable => throw(e)  
    }
  }
} 

