package extractor.plugin
import scala.tools.nsc.Global

object TraversalExtraction{
  
  def apply(global: Global)(unit: global.CompilationUnit) = {
    import global._                      // to have access to typed symbol methods
    
    def ownerChain(node: Node, symbol: Symbol): Unit = {
      if (!node.ownersTraversed)
      {
        println("nameString = " + symbol.nameString)
        println(symbol.nameString != "<root>")
        if (symbol.nameString != "<root>") {
          val ownerSymbol = symbol.owner
          val ownerNode = Nodes(global)(ownerSymbol)
          Edges(symbol.owner.id, "declares member", symbol.id)
          ownerChain(ownerNode, ownerSymbol) 
          node.ownersTraversed = true
        }
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
          
                val node = Nodes(global)(select.symbol)
                
                // record the location where the method is being called - 
                // for making that available to the UI.
                // this is a proof of concept, that only logs it to the console for now.
                if (defParent.isDefined) {
                  val callingSymbol = defParent.get
                  callingSymbol.sourceFile match {
                    case null => 
                      List.empty[String]
                      // TODO: indicate no source for this one
                    case _    =>
                      val source = callingSymbol.sourceFile.toString
                      val line   = select.pos.line
                      val column = select.pos.column
                      println("source location of usage of symbol " + select.symbol.nameString + ": " + source + " " + line + "," + column)
                  }
                }
                
                ownerChain(node, select.symbol)        
                        
                //source(select, Console.YELLOW)
              case _ =>
                if (defParent.isDefined) Edges(defParent.get.id, "uses", select.symbol.id)
                println(defParent.getOrElse("root") + " uses: " + select.symbol + " (" + select.symbol.id + ")" + " of type " + select.symbol.tpe.typeSymbol)
                
                val node = Nodes(global)(select.symbol)
                ownerChain(node, select.symbol)
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
          // This includes capturing the val's type (not kind) - the one that the val instantiates.
          case ValDef(mods: Modifiers, name: TermName, tpt: Tree, rhs: Tree) =>
            val s = tree.symbol
            
            Nodes(global)(s)
            Edges(defParent.get.id, "declares member", s.id)
            
            val valueType = s.tpe.typeSymbol // the type that this val instantiates.
            val node = Nodes(global)(valueType)
            ownerChain(node, valueType)
            
            Edges(s.id, "is of type", valueType.id)
            
            println(defParent.get.id + " has own value: " + s.kindString + " " + s.nameString + " (" + s.id + ") of type " + valueType.id)
            
          // Capture defs of methods.
          // Note this will also capture default constructors synthesized by the compiler
          // and synthetic accessor methods defined by the compiler for vals
          case DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
            val s = tree.symbol
            
            Nodes(global)(s)
            Edges(defParent.get.id, "declares member", s.id)
            
            println(defParent.get.id + " declares own member: " + s.kindString + " " + s.nameString + " (" + s.id + ")")
            
            val traverser = new ExtractAll(Some(tree.symbol))
            traverser.traverse(rhs)
            
          case Template(parents, self, body) =>

            val ts = tree.tpe.typeSymbol
            
            if (ts.nameString == "Serializable") println("Serializable found - " + ts.id)
            if (ts.kindString == "trait") println("trait found - " + ts.id + ' ' + ts.nameString)
            
            val node = Nodes(global)(ts)
            ownerChain(node, ts)
            
            val parentTypeSymbols = parents.map(parent => parent.tpe.typeSymbol).toSet
            parentTypeSymbols.foreach { s => 
              val parentNode = Nodes(global)(s)
              ownerChain(parentNode, s)
            }

            if (defParent.isDefined) Edges(ts.id, "owned by", defParent.get.id) // TODO: move up for readability
              
            parentTypeSymbols.foreach(s => 
              Edges(ts.id, "extends", s.id))
              

            println
            println(tree.tpe.typeSymbol.kindString + " " + tree.tpe.typeSymbol.nameString + " (" + tree.tpe.typeSymbol.id + ") ")
            //sourceSpan(tree)
            
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
