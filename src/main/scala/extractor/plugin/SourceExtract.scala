package extractor.plugin
import tools.nsc.Global

object SourceExtract {
  def apply(global: Global)(symbol: global.Symbol): Option[List[String]] = {

    def getSourceBlock(source: Iterator[String]): List[String] = {
      
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

          body = body :+ line                         // consider a line further indented as belonging
          
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
    
    symbol.sourceFile match {
      case null => 
        None
      case _    =>
        
        if (symbol.pos.toString == "NoPosition") return None // can happen for Scala 2.10 projects, 
                                                             // or just when macros are involved.
        
        val source = symbol.sourceFile.toString
        val line   = symbol.pos.line
        val column = symbol.pos.column
        
        println("source location of symbol " + symbol.nameString + ": " + source + " " + line + "," + column)
        
        if (line == 0) return None  // the compiler provides a line position 0 sometimes,
                                    // whereas line numbers are confirmed to start from 1. 
                                    // Hence we can't extract source here. 

        val sourceCode = scala.io.Source.fromFile(source).getLines
        val (sourceIterator1, sourceIterator2) = sourceCode.duplicate
        val defLine = sourceIterator1.toArray.apply(line-1)
        
        val block = getSourceBlock(sourceIterator2.drop(line-1))
        
        println(Console.BLUE + Console.BOLD + defLine + Console.RESET)
        //println(symbol.pos.lineCaret + Console.RESET)
        Some(block)
    } 
  }
}