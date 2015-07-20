package extractor.plugin
import scala.tools.nsc.Global

object DependencyExtraction{
  
  def apply(global: Global)(unit: global.CompilationUnit) = {
    import global._

    def ownerChain(symbol: Symbol): Unit = {
      val owner = symbol.owner
      if (Nodes(owner.id, owner.nameString, owner.kindString)) {
        Edges(owner.id, "declares member", symbol.id)
        println(owner + " xdeclares " + symbol)
      }
      if (symbol.owner.nameString != "<root>") {
        ownerChain(symbol.owner) 
      }
    }
    
    class ExtractSourceSpan extends Traverser {
      override def traverse(tree: Tree): Unit = {
        //println(Console.RED + Console.BOLD + tree.pos.isRange + Console.RESET)
        println(Console.RED +  
                "line " + tree.pos.line + ": " + 
                Console.BOLD + tree.pos.lineContent + Console.RESET)
      }
    }
    
    def sourceSpan(tree: Tree, color: String = Console.BLUE) = {

      val traverser = new ExtractSourceSpan
      traverser.traverse(tree)
      println
            
      /*
      val symbol = tree.symbol
      println(color + Console.BOLD + "source file: " + symbol.pos.source)
      println("source file full path: " + symbol.sourceFile)  
      println("line: " + symbol.pos.line)  
      println("line: " + symbol.pos.lineContent)  
      if (symbol.pos.isRange) println("range start: " + symbol.pos.start + " end: " + symbol.pos.end)
      
      //println(unit.body)
      
      //tree.children.foreach(t => println(t.pos.lineContent))
      
      //unit.echo(symbol.pos, "blaaaaaaaaaaa")
      
      println(Console.RESET)
      */
    }
    
    class ExtractAll(defParent: Option[global.Symbol]) extends Traverser {
      override def traverse(tree: Tree): Unit = {

        def extractSource(symbol: Symbol) = {
          
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
          
          if (symbol.sourceFile != null) {
            val source = symbol.sourceFile.toString
            val line   = symbol.pos.line
            val column = symbol.pos.column
            
            val sourceCode = scala.io.Source.fromFile(source).getLines
            val (sourceIterator1, sourceIterator2) = sourceCode.duplicate
            val defLine = sourceIterator1.toArray.apply(line-1)
            
            val block = getSourceBlock(sourceIterator2.drop(line-1))
            
            println(Console.BLUE + Console.BOLD)
            println("file   =| " + "file://" + source)
            println("symbol =| " + symbol)
            println("line   =| " + line)
            println("column =| " + column)
            println
            if (!symbol.nameString.contains("$anon"))
              println(" " * defLine.indexWhere(c => c !=' ') + "v ")
            else 
              println(" " * defLine.indexWhere(c => c !=' ') + "v ")
            println(defLine)
            println(symbol.pos.lineCaret)
            //println(defLine.indexWhere(c => c !=' '))
            println(Console.RESET)

            println(block.mkString("\n"))
          }
        }
    
        tree match {
          case i @ Import(expr, selectors) =>
            selectors.foreach {
              case ImportSelector(nme.WILDCARD, _, null, _) => // a wildcard import
                // in case of wildcard import we do not rely on any particular name being defined
                // on `expr`; all symbols that are being used will get caught through selections
                // println(tree)
              case ImportSelector(name: Name, _, _, _) => // a specific import
                def lookupImported(name: Name) = expr.symbol.info.member(name)
                // importing a name means importing both a term and a type (if they exist)
                // println(tree); println(lookupImported(name) + "(" + lookupImported(name).id + ")")
            }
            
          case select: Select =>
            select.symbol.kindString match {
              case "constructor" => // ignore
              case "method" =>
                if (defParent.isDefined) Edges(defParent.get.id, "uses", select.symbol.id)
                
                println(defParent.getOrElse("root") + " uses: " + select.symbol + " (" + select.symbol.id + ")" +  
                        " of " + select.symbol.owner + " owned by " + select.symbol.owner.owner +
                        " owned by " + select.symbol.owner.owner.owner +
                        " (" + select.symbol.owner.owner.owner.id + ")" +
                        select.symbol.owner.owner.owner.owner  
                        + " (" + select.symbol.owner.owner.owner.owner.id + ")" +
                        select.symbol.owner.owner.owner.owner.owner +  
                        " (" + select.symbol.owner.owner.owner.owner.owner.id + ")" 

                        )
          
                extractSource(select.symbol)
                Nodes(select.symbol.id, select.symbol.nameString, select.symbol.kindString)
                ownerChain(select.symbol)        
                        
                //source(select, Console.YELLOW)
              case _ =>
                if (defParent.isDefined) Edges(defParent.get.id, "uses", select.symbol.id)
                println(defParent.getOrElse("root") + " uses: " + select.symbol + " (" + select.symbol.id + ")" + " of type " + select.symbol.tpe.typeSymbol)
                
                extractSource(select.symbol)
                Nodes(select.symbol.id, select.symbol.nameString, select.symbol.kindString)
                ownerChain(select.symbol)
                //source(select, Console.YELLOW)
            }
            
            //select.symbol.tpe.typeSymbol + " or rather of " + select.symbol.owner)
            
          /*
           * Idents are used in number of situations:
           *  - to refer to local variable
           *  - to refer to a top-level package (other packages are nested selections)
           *  - to refer to a term defined in the same package as an enclosing class;
           *    this looks fishy, see this thread:
           *    https://groups.google.com/d/topic/scala-internals/Ms9WUAtokLo/discussion
           */
          case ident: Ident => //println(ident.symbol)
          case typeTree: TypeTree  => // are we missing something by not handling this?
          
          // Captures val definitions rather than their automatic accessor methods..
          case ValDef(mods: Modifiers, name: TermName, tpt: Tree, rhs: Tree) =>
            val s = tree.symbol
            
            extractSource(s)
            
            Nodes(s.id, s.nameString, s.kindString)
            Edges(defParent.get.id, "has own value", s.id)
            
            println(defParent.get.id + " has own value: " + s.kindString + " " + s.nameString + " (" + s.id + ")")
            
            
          // Capture defs of methods.
          // Note this will also capture default constructors synthesized by the compiler
          // and synthetic accessor methods defined by the compiler for vals
          case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
            val s = tree.symbol
            
            extractSource(s)
            
            if (Nodes(s.id, s.nameString, s.kindString))
              Edges(defParent.get.id, "declares member", s.id)
            
            println(defParent.get.id + " declares own member: " + s.kindString + " " + s.nameString + " (" + s.id + ")")
            
            val traverser = new ExtractAll(Some(tree.symbol))
            traverser.traverse(rhs)
            
          case Template(parents, self, body) =>

            val ts = tree.tpe.typeSymbol
            
            extractSource(ts)
            
            Nodes(ts.id, ts.nameString, ts.kindString)
            
            val parentTypeSymbols = parents.map(parent => parent.tpe.typeSymbol).toSet
            parentTypeSymbols.foreach(s => 
              Nodes(s.id, s.nameString, s.kindString))

            if (defParent.isDefined) Edges(ts.id, "owned by", defParent.get.id)  
              
            parentTypeSymbols.foreach(s => 
              Edges(ts.id, "extends", s.id))

            println
            println(tree.tpe.typeSymbol.kindString + " " + tree.tpe.typeSymbol.nameString + " (" + tree.tpe.typeSymbol.id + ") ")
            //sourceSpan(tree)
            ownerChain(ts)
            
            parentTypeSymbols.foreach(s => println("extends: " + s.kindString + " " + s.nameString + " (" + s.id + ") "))
            
            //tree.tpe.declarations.foreach(s => println("declares own member: " + s.kindString + " " + s.nameString + " (" + s.id + ")")) // TODO: need to deduplicate these
            
            val traverser = new ExtractAll(Some(tree.tpe.typeSymbol))
            body foreach { tree =>
              traverser.traverse(tree)
            }
          case tree =>
            super.traverse(tree) // println(" =========> general traverse call ")
        }
      }
    }
    
    val traverser = new ExtractAll(None)
    traverser.traverse(unit.body)
    
    println
    //Nodes.list.map(node => println(node._2))
    println("total " + Nodes.list.size + " nodes")
    //Edges.list.map(println)
    println("total " + Edges.list.size + " edges")
    Output.write
    
    Unit // this statement guards against a compiler crash
  }
}
