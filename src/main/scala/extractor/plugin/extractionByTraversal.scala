package extractor.plugin
import scala.tools.nsc.Global

object DependencyExtraction{
  
  def apply(global: Global)(unit: global.CompilationUnit) = {
    import global._

    def ownerChain(symbol: Symbol): Unit = {
      val owner = symbol.owner
      //println("=| " + owner + " " + owner.id + " owns " + symbol + " " + symbol.id + " |=")
      Nodes(owner.id, owner.nameString, owner.keyString)
      Edges(owner.id, "declares member", symbol.id)
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
                
                ownerChain(select.symbol)        
                        
                //source(select, Console.YELLOW)
              case _ =>
                if (defParent.isDefined) Edges(defParent.get.id, "uses", select.symbol.id)
                println(defParent.getOrElse("root") + " uses: " + select.symbol + " of type " + select.symbol.tpe.typeSymbol)
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
          
          case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
            val s = tree.symbol
            
            Nodes(s.id, s.nameString, s.keyString)
            Edges(defParent.get.id, "declares member", s.id)
            
            println(defParent.get.id + " declares own member: " + s.kindString + " " + s.nameString + " (" + s.id + ")")
            //sourceSpan(tree, Console.GREEN)
            
            val traverser = new ExtractAll(Some(tree.symbol))
            traverser.traverse(rhs)
            
          case Template(parents, self, body) =>

            val ts = tree.tpe.typeSymbol
            Nodes(ts.id, ts.nameString, ts.keyString)
            
            val parentTypeSymbols = parents.map(parent => parent.tpe.typeSymbol).toSet
            parentTypeSymbols.foreach(s => 
              Nodes(s.id, s.nameString, s.keyString))

            if (defParent.isDefined) Edges(ts.id, "owned by", defParent.get.id)  
              
            parentTypeSymbols.foreach(s => 
              Edges(ts.id, "extends", s.id))

            println
            println(tree.tpe.typeSymbol.keyString + " " + tree.tpe.typeSymbol.nameString + " (" + tree.tpe.typeSymbol.id + ") ")
            //sourceSpan(tree)
            if (defParent.isDefined) println("is owned by " + defParent.get.id)
            parentTypeSymbols.foreach(s => println("extends: " + s.keyString + " " + s.nameString + " (" + s.id + ") "))
            
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
