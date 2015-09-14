package extractor.plugin
import tools.nsc.Global

// TODO: add capturing leading comment lines, as a separate `comments` property

object SourceExtract {
  
  private def getSourceBlockHeuristic(source: Iterator[String]): List[String] = {

    def getStartCol(s: String) = s.indexWhere(c => c !=' ')
   
    var body: List[String] = List(source.next)
    var done = false
    
    var inBracesNest  = 0
    var inQuote       = false
    
    val initialStartCol = getStartCol(body.head)
    
    while(source.hasNext && !done) {
      val line = source.next
      val startCol = getStartCol(line)
      
      if (startCol > initialStartCol) {  
        
        for (char <- line) // keep track of block nesting level,
                           // in case we want to use it later
          if (!inQuote) char match {
          case '{' => inBracesNest += 1
          case '}' => inBracesNest -= 1
          case '"' => inQuote = !inQuote
          case _ =>
          }

        body = body :+ line                  // consider a line further indented as belonging
        
      }
      else if (startCol == initialStartCol) 
        if (line(startCol) == '}') {         // consider first closing brace at initial indentation column
                                             // as the last line to belong
          body = body :+ line
          done = true
        }
        else 
          done = true                        // a line that is indented same as the initial indentation
                                             // indentation, but doesn't brace-close it, means we're done
                                             // (e.g. think a case class without an explicit body)
    }
    body
    
  }
  
  def apply(global: Global)(symbol: global.Symbol): Option[String] = {
    
    symbol.sourceFile match {
      case null => None
      case _    =>
        
        // a lot of guard statements which are all necessary given all kinds of special cases
        
        if (symbol.isSynthetic) return None                   

        if (symbol.pos.toString == "NoPosition") return None // can happen for Scala 2.10 projects, 
                                                             // or just when macros are involved.
        val source = symbol.sourceFile.toString
        val line   = symbol.pos.line
        val column = symbol.pos.column
        val start  = symbol.pos.start
        val end    = symbol.pos.end

        if (line == 0) return None  // the compiler provides a line position 0 sometimes,
                                    // whereas line numbers are confirmed to start from 1. 
                                    // Hence we can't extract source here.         
        
        if (start == end) return None // this also means it is effectively a synthetic definition
                                      // even though .isSynthethic == false (!), which is the case
                                      // with a body-less case class in scala 2.11.        


        /*
         * heuristic based extraction - may be necessary for supporting scala 2.10 so keep it alive
         * (c.f. https://github.com/scoverage/scalac-scoverage-plugin/blob/5d0c92479dff0055f2cf7164439f838b803fe44a/2.10.md)
         */
        val blockFromHeuristic = getSourceBlockHeuristic(scala.io.Source.fromFile(source).getLines.drop(line-1))        
        
        /*
         * Extract the source code of the symbol using compiler supplied ranges 
         *
         * In the obvious case it just mirrors the original indentation in the source code,  
         * which is not included in the range given by the compiler, so it is here "added it back". 
         * 
         * In the less obvious case a definition may not start on a line of its own in the source
         * code (think anonymous definitions). In that case unrelated preceding text will be removed and 
         * replaced by leading spaces rather than only removed. Not optimal for anonymous classes
         * defined with the `new` keyword, but that's a compiler story... 
         */      
        val block = {
          val sourceCode  = scala.io.Source.fromFile(source).mkString
          val firstLineIdentLength = sourceCode.slice(0, start).reverse.takeWhile(_ != '\n').length   
          " " * firstLineIdentLength + sourceCode.slice(start, end)
        }
        
        //println(Console.BLUE + Console.BOLD + defLine + Console.RESET)
        //println(symbol.pos.lineCaret + Console.RESET)
        Some(block)
    } 
  }
}