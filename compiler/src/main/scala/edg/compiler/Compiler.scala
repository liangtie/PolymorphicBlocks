package edg.compiler

import edg.EdgirUtils._
import edg.util.{DependencyGraph, Errorable, SingleWriteHashMap}
import edg.wir._
import edg.wir
import edgir.expr.expr
import edgir.ref.ref
import edgir.schema.schema

import scala.collection.{SeqMap, mutable}


class IllegalConstraintException(msg: String) extends Exception(msg)


sealed trait ElaborateRecord
object ElaborateRecord {
  sealed trait ElaborateTask extends ElaborateRecord  // an elaboration task that can be run
  sealed trait ElaborateDependency extends ElaborateRecord  // an elaboration dependency source

  case class Block(blockPath: DesignPath) extends ElaborateTask  // even when done, still may only be a generator
  case class Link(linkPath: DesignPath) extends ElaborateTask
  case class LinkArray(linkPath: DesignPath) extends ElaborateTask

  // Connection to be elaborated, to set port parameter, IS_CONNECTED, and CONNECTED_LINK equivalences.
  // Only elaborates the direct connect, and for bundles, creates sub-Connect tasks since it needs
  // connectedLink and linkParams.
  case class Connect(toLinkPortPath: DesignPath, toBlockPortPath: DesignPath)
      extends ElaborateTask

  // Elaborates the contents of a port array, based on the port array's ELEMENTS parameter.
  // For recursive arrays, (eg, in link arrays), this is set when recursively elaborated.
  // Only called for port arrays without defined elements (so excluding blocks that define their ports, including
  // generator-defined port arrays which are structurally similar).
  // Created but never run for abstract blocks with abstract port array.
  case class ElaboratePortArray(path: DesignPath) extends ElaborateTask

  case class ParamValue(paramPath: IndirectDesignPath) extends ElaborateDependency  // when solved

  // Set when the connection from the link's port to portPath have been elaborated, or for link ports
  // when the link has been elaborated.
  // When this is completed, connectedLink for the port and linkParams for the link will be set.
  // Never set for port arrays.
  case class ConnectedLink(portPath: DesignPath) extends ElaborateDependency

  // The next tasks are a series for array connection

  // Defines the ALLOCATED for the port array, by aggregating all the connected ports.
  // Requires: ELEMENTS of all incoming connections defined.
  case class ResolveArrayAllocated(parent: DesignPath, portPath: Seq[String], constraintNames: Seq[String],
                                   arrayConstraintNames: Seq[String], portIsLink: Boolean)
      extends ElaborateTask with ElaborateDependency

  // For array connections (to link arrays) only, rewrites constraints to replace the ALLOCATE with a concrete
  // port name, automatically allocated.
  // Requires: array-connections expanded, port's ELEMENTS defined.
  case class RewriteArrayAllocate(parent: DesignPath, portPath: Seq[String], constraintNames: Seq[String],
                                  arrayConstraintNames: Seq[String], portIsLink: Boolean)
      extends ElaborateTask with ElaborateDependency

  // Expands ArrayConnect and ArrayExport connections to individual Connect and Export operations.
  // ALLOCATEs are preserved as-is, meaning those will be allocated on an individual (instead of array) basis.
  // Requires: port's ELEMENTS defined.
  case class ExpandArrayConnections(parent: DesignPath, constraintName: String)
      extends ElaborateTask with ElaborateDependency

  // Once lowered to single connects, rewrites constraints to replace the ALLOCATE with a concrete port name,
  // allocated from the port's ELEMENTS.
  // Requires: array-connections expanded, port's ELEMENTS defined.
  case class RewriteConnectAllocate(parent: DesignPath, portPath: Seq[String], constraintNames: Seq[String],
                                    arrayConstraintNames: Seq[String], portIsLink: Boolean)
      extends ElaborateTask with ElaborateDependency

  // Sets a PortArray's IS_CONNECTED based off all the connected constraints.
  // Requires: array-connections expanded, ALLOCATE replaced with concrete indices
  case class ResolveArrayIsConnected(parent: DesignPath, portPath: Seq[String], constraintNames: Seq[String],
                                     arrayConstraintNames: Seq[String], portIsLink: Boolean)
      extends ElaborateTask
}


sealed trait CompilerError
object CompilerError {
  case class Unelaborated(elaborateRecord: ElaborateRecord, missing: Set[ElaborateRecord]) extends CompilerError {
    // These errors may be redundant with below, but provides dependency data
    override def toString: String = s"Unelaborated missing dependencies $elaborateRecord:\n" +
        s"${missing.map(x => s"- $x").mkString("\n")}"
  }
  case class LibraryElement(path: DesignPath, target: ref.LibraryPath) extends CompilerError {
    override def toString: String = s"Unelaborated library element ${target.toSimpleString} @ $path"
  }
  case class UndefinedPortArray(path: DesignPath, portType: ref.LibraryPath) extends CompilerError {
    override def toString: String = s"Undefined port array ${portType.toSimpleString} @ $path"
  }

  case class LibraryError(path: DesignPath, target: ref.LibraryPath, err: String) extends CompilerError {
    override def toString: String = s"Library error ${target.toSimpleString} @ $path: $err"
  }
  case class GeneratorError(path: DesignPath, target: ref.LibraryPath, err: String) extends CompilerError {
    override def toString: String = s"Generator error ${target.toSimpleString} @ $path: $err"
  }
  case class RefinementSubclassError(path: DesignPath, refinedLibrary: ref.LibraryPath, designLibrary: ref.LibraryPath)
      extends CompilerError {
    override def toString: String =
      s"Invalid refinement ${refinedLibrary.toSimpleString} <- ${designLibrary.toSimpleString} @ $path"
  }

  case class OverAssign(target: IndirectDesignPath,
                        causes: Seq[OverAssignCause]) extends CompilerError {
    override def toString: String = s"Overassign to $target:\n" +
        s"${causes.map(x => s"- $x").mkString("\n")}"
  }

  case class BadRef(path: DesignPath, ref: IndirectDesignPath) extends CompilerError

  case class AbstractBlock(path: DesignPath, blockType: ref.LibraryPath) extends CompilerError {
    override def toString: String = s"Abstract block: $path (of type ${blockType.toSimpleString})"
  }

  case class FailedAssertion(root: DesignPath, constrName: String,
                             value: expr.ValueExpr, result: ExprValue) extends CompilerError {
    override def toString: String =
      s"Failed assertion: $root.$constrName, ${ExprToString.apply(value)} => $result"
  }
  case class MissingAssertion(root: DesignPath, constrName: String,
                              value: expr.ValueExpr, missing: Set[IndirectDesignPath]) extends CompilerError {
    override def toString: String =
      s"Unevaluated assertion: $root.$constrName (${ExprToString.apply(value)}), missing ${missing.mkString(", ")}"
  }

  // TODO should this be an error? Currently a debugging tool
  case class EmptyRange(param: IndirectDesignPath, root: DesignPath, constrName: String,
                        value: expr.ValueExpr) extends CompilerError

  sealed trait OverAssignCause
  object OverAssignCause {
    case class Assign(target: IndirectDesignPath, root: DesignPath, constrName: String, value: expr.ValueExpr)
        extends OverAssignCause {
      override def toString = s"Assign $target <- ${ExprToString(value)} @ $root.$constrName"
    }
    case class Equal(target: IndirectDesignPath, source: IndirectDesignPath)  // TODO constraint info once we track that?
        extends OverAssignCause {
      override def toString = s"Equals $target = $source"
    }
  }
}


/** Compiler for a particular design, with an associated library to elaborate references from.
  * TODO also needs a Python interface for generators, somewhere.
  *
  * During the compilation process, internal data structures are mutated.
  *
  * Port parameters are propagated by expanding connect and export statements between connected ports
  * into equalities between all contained parameters.
  * This expansion triggers when both ports are fully elaborated, and checks the structures of both ends
  * for equivalence.
  *
  * CONNECTED_LINK parameters are propagated by expanding from the link's top-level port outward.
  * Expansion triggers at the link-side top-level port (by ref matching), or when the towards-innermost-link
  * (or towards-outermost-block) port is expanded.
  * A list of link params is kept in a hashmap indexed by ports, as they are expanded.
  *
  * Alternative: fetch links from library (using the port type) to get params to expand.
  * Problem: a bit more restrictive than what can be expressed in a block - but should be a common interface.
  *
  * It is intentional to allow a link-side port to access the CONNECTED_LINK, as a mechanism to access inner links.
  */
class Compiler(inputDesignPb: schema.Design, library: edg.wir.Library,
               refinements: Refinements=Refinements()) {
  def readableLibraryPath(path: ref.LibraryPath): String = {  // TODO refactor to shared utils?
    path.getTarget.getName
  }

  // Working design tree data structure
  private val root = new wir.Block(inputDesignPb.getContents, None)  // TODO refactor to unify root / non-root cases
  def resolve(path: DesignPath): wir.Pathable = root.resolve(path.steps)
  def resolveBlock(path: DesignPath): wir.BlockLike = root.resolve(path.steps).asInstanceOf[wir.BlockLike]
  def resolveLink(path: DesignPath): wir.LinkLike = root.resolve(path.steps).asInstanceOf[wir.LinkLike]
  def resolvePort(path: DesignPath): wir.PortLike = root.resolve(path.steps).asInstanceOf[wir.PortLike]

  // Main data structure that tracks the next unit to elaborate
  private val elaboratePending = DependencyGraph[ElaborateRecord, None.type]()

  // Design parameters solving (constraint evaluation) and assertions
  private val constProp = new ConstProp() {
    override def onParamSolved(param: IndirectDesignPath, value: ExprValue): Unit = {
      elaboratePending.setValue(ElaborateRecord.ParamValue(param), null)
    }
  }
  private[edg] def getValue(path: IndirectDesignPath): Option[ExprValue] = constProp.getValue(path)  // TODO clean up this API?
  for ((path, value) <- refinements.instanceValues) {  // seed const prop with path assertions
    constProp.setForcedValue(path.asIndirect, value, "path refinement")
  }

  private val assertions = mutable.Buffer[(DesignPath, String, expr.ValueExpr, SourceLocator)]()  // containing block, name, expr

  // Supplemental elaboration data structures
  private val linkParams = SingleWriteHashMap[DesignPath, Seq[IndirectStep]]()  // link path -> list of params
  linkParams.put(DesignPath(), Seq())  // empty path means disconnected
  private val connectedLink = SingleWriteHashMap[DesignPath, DesignPath]()  // port -> connected link path
  private val expandedArrayConnectConstraints = SingleWriteHashMap[DesignPath, Seq[String]]()  // constraint path -> new constraint names

  private val errors = mutable.ListBuffer[CompilerError]()

  // Returns all errors, by scanning the design tree for errors and adding errors accumulated through the compile
  // process
  def getErrors(): Seq[CompilerError] = {
    val assertionErrors = assertions.flatMap { case (root, constrName, value, sourceLocator) =>
      new ExprEvaluatePartial(constProp, root).map(value) match {
        case ExprResult.Result(BooleanValue(true)) => None
        case ExprResult.Result(result) =>
          Some(CompilerError.FailedAssertion(root, constrName, value, result))
        case ExprResult.Missing(missing) =>
          Some(CompilerError.MissingAssertion(root, constrName, value, missing))
      }
    }.toSeq

    val pendingErrors = elaboratePending.getMissing.map { missingNode =>
      CompilerError.Unelaborated(missingNode, elaboratePending.nodeMissing(missingNode))
    }.toSeq

    errors.toSeq ++ constProp.getErrors ++ pendingErrors ++ assertionErrors
  }

  // Seed the elaboration record with the root design
  //
  elaboratePending.addNode(ElaborateRecord.Block(DesignPath()), Seq())
  require(root.getPorts.isEmpty, "design top may not have ports")  // also don't need to elaborate top ports
  processParamDeclarations(DesignPath(), root)

  // Hook method to be overridden, eg for status
  //
  def onElaborate(record: ElaborateRecord): Unit = { }

  // Actual compilation methods
  //

  // Elaborate a connection (either a connect or export), by generating bidirectional equality constraints
  // including link parameters (through CONNECTED_LINK) and IS_CONNECTED.
  // Neither port can be an array (container) port (array connects should be expanded into individual element connects),
  // but ports may be bundle or inner ports (in bundles or arrays).
  // Only the link side port needs to have been elaborated.
  protected def elaborateConnect(connect: ElaborateRecord.Connect): Unit = {
    // Generate port-port parameter propagation
    // All connected ports should have params
    val toLinkPort = resolvePort(connect.toLinkPortPath).asInstanceOf[wir.HasParams]
    val connectedParam = toLinkPort.getParams.keys.map(IndirectStep.Element(_))
    for (connectedStep <- connectedParam) { // note: can't happen for top level connect!
      constProp.addEquality(
        connect.toLinkPortPath.asIndirect + connectedStep,
        connect.toBlockPortPath.asIndirect + connectedStep
      )
    }

    val linkPath = connectedLink(connect.toLinkPortPath)  // must have ben set with ConnectedLink
    connectedLink.put(connect.toBlockPortPath, linkPath)  // propagate CONNECTED_LINK params
    val allParams = linkParams(linkPath) :+ IndirectStep.Name
    for (linkParam <- allParams) {  // generate CONNECTED_LINK equalities
      constProp.addEquality(
        connect.toBlockPortPath.asIndirect + IndirectStep.ConnectedLink + linkParam,
        linkPath.asIndirect + linkParam,
      )
    }

    // Add sub-ports to the elaboration dependency graph, as appropriate
    toLinkPort match {
      case toLinkPort: wir.Bundle =>
        for (portName <- toLinkPort.getPorts.keys) {
          constProp.addEquality(connect.toLinkPortPath.asIndirect + IndirectStep.IsConnected,
            connect.toLinkPortPath.asIndirect + portName + IndirectStep.IsConnected)
          constProp.addEquality(connect.toBlockPortPath.asIndirect + IndirectStep.IsConnected,
            connect.toBlockPortPath.asIndirect + portName + IndirectStep.IsConnected)

          elaboratePending.addNode(
            ElaborateRecord.Connect(connect.toLinkPortPath + portName, connect.toBlockPortPath + portName),
            Seq(ElaborateRecord.ConnectedLink(connect.toLinkPortPath + portName))
          )
        }
      case toLinkPort => // everything else ignored
    }

    // Register port as finished
    elaboratePending.setValue(ElaborateRecord.ConnectedLink(connect.toBlockPortPath), None)
  }

  protected def resolvePortConnectivity(containerPath: DesignPath, portPostfix: Seq[String],
                                        constraint: Option[(String, expr.ValueExpr)]): Unit = {
    val port = resolvePort(containerPath ++ portPostfix)
    val container = resolve(containerPath).asInstanceOf[wir.HasMutableConstraints]  // block or link
    val portBlock = resolve(containerPath + portPostfix.head).asInstanceOf[wir.HasMutableConstraints]  // block or link
    val constraintExpr = constraint.map { case (constrName, constr) => (constrName, constr.expr) }

    def recursiveSetNotConnected(portPath: DesignPath, port: wir.PortLike): Unit = {
      constProp.setValue(portPath.asIndirect + IndirectStep.IsConnected,
        BooleanValue(false),
        s"${containerPath ++ portPostfix}.(not connected)")
      portBlock match {
        case _: wir.Block =>
          connectedLink.put(portPath, DesignPath())
          elaboratePending.setValue(ElaborateRecord.ConnectedLink(portPath), None)
        case _: wir.Link | _: wir.LinkArray =>  // links set these on all ports, so this is ignored here. TODO: unify code paths?
      }
      port match {
        case port: wir.Bundle =>
          port.getPorts.foreach { case (innerIndex, innerPort) =>
            recursiveSetNotConnected(portPath + innerIndex, innerPort)
          }
        case _ =>  // no recursion at leaf
      }
    }

    port match {
      case _: wir.Bundle | _: wir.Port => constraintExpr match {
        case Some((constrName, expr.ValueExpr.Expr.Connected(connected))) =>
          require(container.isInstanceOf[wir.Block])
          constProp.setValue(containerPath.asIndirect ++ portPostfix + IndirectStep.IsConnected,
            BooleanValue(true),
            s"$containerPath.$constrName")
        case Some((constrName, expr.ValueExpr.Expr.Exported(exported))) =>
          constProp.addDirectedEquality(containerPath.asIndirect ++ portPostfix + IndirectStep.IsConnected,
            containerPath.asIndirect ++ exported.getExteriorPort.getRef + IndirectStep.IsConnected,
            containerPath, s"$containerPath.$constrName")
        case None =>
          recursiveSetNotConnected(containerPath ++ portPostfix, port)
        case Some((_, _)) => throw new IllegalArgumentException
      }
      case _: wir.PortLibrary => throw new IllegalArgumentException
      case _: wir.PortArray => throw new IllegalArgumentException  // must be lowered before
    }
  }

  // Called for each param declaration, currently just registers the declaration and type signature.
  protected def processParamDeclarations(path: DesignPath, hasParams: wir.HasParams): Unit = {
    for ((paramName, param) <- hasParams.getParams) {
      constProp.addDeclaration(path + paramName, param)
    }
  }

  // Elaborates the port, mutating it in-place. Recursive.
  protected def elaboratePort(path: DesignPath, container: wir.HasMutablePorts, port: wir.PortLike): Unit = {
    // Instantiate as needed
    val instantiated = port match {
      case port: wir.PortLibrary =>
        val libraryPath = port.target

        val portPb = library.getPort(libraryPath) match {
          case Errorable.Success(portPb) => portPb
          case Errorable.Error(err) =>
            import edg.IrPort
            import edgir.elem.elem
            errors += CompilerError.LibraryError(path, libraryPath, err)
            IrPort.Port(elem.Port())
        }
        val newPort = wir.PortLike.fromIrPort(portPb)
        container.elaborate(path.lastString, newPort)
        newPort
      case port: wir.PortArray => port  // no instantiation needed
      case port => throw new NotImplementedError(s"unknown unelaborated port $port")
    }

    // Process and recurse as needed
    instantiated match {
      case port: wir.Port =>
        constProp.setValue(path.asIndirect + IndirectStep.Name, TextValue(path.toString))
        processParamDeclarations(path, port)
      case port: wir.Bundle =>
        constProp.setValue(path.asIndirect + IndirectStep.Name, TextValue(path.toString))
        processParamDeclarations(path, port)
        for ((childPortName, childPort) <- port.getPorts) {
          elaboratePort(path + childPortName, port, childPort)
        }
      case port: wir.PortArray =>
        if (port.portsSet) {  // set ELEMENTS if ports is defined by array, otherwise ports are dependent on ELEMENTS
          constProp.setValue(path.asIndirect + IndirectStep.Elements,
            ArrayValue(port.getPorts.keys.toSeq.map(TextValue(_))))
        }
        elaboratePending.addNode(ElaborateRecord.ElaboratePortArray(path), Seq(  // does recursive elaboration + LENGTH
          ElaborateRecord.ParamValue(path.asIndirect + IndirectStep.Elements)
        ))
      case port => throw new NotImplementedError(s"unknown instantiated port $port")
    }
  }

  // Attempts to process a parameter constraint, returning true if it is a matching constraint
  def processParamConstraint(blockPath: DesignPath, constrName: String, constr: expr.ValueExpr,
                             constrValue: expr.ValueExpr): Boolean = constrValue.expr match {
    case expr.ValueExpr.Expr.Assign(assign) =>
      constProp.addAssignment(
        blockPath.asIndirect ++ assign.dst.get,
        blockPath, assign.src.get, constrName) // TODO add sourcelocators
      true
    case expr.ValueExpr.Expr.Binary(_) | expr.ValueExpr.Expr.BinarySet(_) |
        expr.ValueExpr.Expr.Unary(_) | expr.ValueExpr.Expr.UnarySet(_) |
        expr.ValueExpr.Expr.IfThenElse(_) =>  // raw ValueExprs interpreted as assertions
      assertions += ((blockPath, constrName, constr, SourceLocator.empty))  // TODO add source locators
      true
    case expr.ValueExpr.Expr.Ref(target)  // IsConnected also treated as assertion
      if target.steps.last.step.isReservedParam
          && target.steps.last.getReservedParam == ref.Reserved.IS_CONNECTED =>
      assertions += ((blockPath, constrName, constr, SourceLocator.empty))  // TODO add source locators
      true
    case _ => false
  }

  // Attempts to process a connected constraint, returning true if it is a matching constraint
  def processConnectedConstraint(blockPath: DesignPath, constrName: String, constr: expr.ValueExpr,
                                 isInLink: Boolean): Boolean = {
    import edg.ExprBuilder.ValueExpr
    constr.expr match {
      case expr.ValueExpr.Expr.Connected(connected) => (connected.getBlockPort, connected.getLinkPort) match {
        case (ValueExpr.Ref(blockPort), ValueExpr.Ref(linkPort)) =>
          require(!isInLink)
          elaboratePending.addNode(
            ElaborateRecord.Connect(blockPath ++ linkPort, blockPath ++ blockPort),
            Seq(ElaborateRecord.ConnectedLink(blockPath ++ linkPort))
          )
          true
        case _ => false  // anything with allocates is not processed
      }
      case expr.ValueExpr.Expr.Exported(exported) => (exported.getExteriorPort, exported.getInternalBlockPort) match {
        case (ValueExpr.Ref(extPort), ValueExpr.Ref(intPort)) =>
          if (!isInLink) {
            elaboratePending.addNode(
              ElaborateRecord.Connect(blockPath ++ extPort, blockPath ++ intPort),
              Seq(ElaborateRecord.ConnectedLink(blockPath ++ extPort))
            )
          } else {  // for links, the external port faces to the block, so args must be flipped
            elaboratePending.addNode(
              ElaborateRecord.Connect(blockPath ++ intPort, blockPath ++ extPort),
              Seq(ElaborateRecord.ConnectedLink(blockPath ++ intPort))
            )
          }
          true
        case _ => false  // anything with allocates is not processed
      }
      case _ => false  // not defined
    }
  }

  // Given a block library at some path, expand it and return the instantiated block.
  // Handles class type refinements and adds default parameters and class-based value refinements
  // For the generator, this will be a skeleton block.
  protected def expandBlock(path: DesignPath, block: wir.BlockLibrary): wir.Block = {
    val libraryPath = block.target

    val (refinedLibraryPath, unrefinedType) = refinements.instanceRefinements.get(path) match {
      case Some(refinement) => (refinement, Some(libraryPath))
      case None => refinements.classRefinements.get(libraryPath) match {
        case Some(refinement) => (refinement, Some(libraryPath))
        case None => (libraryPath, None)
      }
    }

    val blockPb = library.getBlock(refinedLibraryPath) match {
      case Errorable.Success(blockPb) =>
        blockPb
      case Errorable.Error(err) =>
        import edgir.elem.elem
        errors += CompilerError.LibraryError(path, refinedLibraryPath, err)
        elem.HierarchyBlock()
    }

    // additional processing needed for the refinement case
    if (unrefinedType.isDefined) {
      if (!library.isSubclassOf(refinedLibraryPath, libraryPath)) {  // check refinement validity
        errors += CompilerError.RefinementSubclassError(path, refinedLibraryPath, libraryPath)
      }

      val unrefinedPb = library.getBlock(libraryPath) match {  // add subclass (refinement) default params
        case Errorable.Success(unrefinedPb) =>
          unrefinedPb
        case Errorable.Error(err) =>  // this doesn't stop elaboration, but does raise an error
          import edgir.elem.elem
          errors += CompilerError.LibraryError(path, libraryPath, err)
          elem.HierarchyBlock()
      }
      val refinedNewParams = blockPb.params.keys.toSet -- unrefinedPb.params.keys
      refinedNewParams.foreach { refinedNewParam =>
        blockPb.paramDefaults.get(refinedNewParam).foreach { refinedDefault =>
          constProp.addAssignment(path.asIndirect + refinedNewParam, path, refinedDefault,
            s"(default)${refinedLibraryPath.toSimpleString}.$refinedNewParam")
        }
      }
    }

    // add class-based refinements
    refinements.classValues.get(refinedLibraryPath).foreach { classValueRefinements =>
      for ((subpath, value) <- classValueRefinements) {
        constProp.setForcedValue(path.asIndirect ++ subpath, value,
          s"${refinedLibraryPath.getTarget.getName} class refinement")
      }
    }

    val newBlock = if (blockPb.generator.isEmpty) {
      new wir.Block(blockPb, unrefinedType)
    } else {
      new wir.Generator(blockPb, unrefinedType)
    }

    constProp.setValue(path.asIndirect + IndirectStep.Name, TextValue(path.toString))
    processParamDeclarations(path, newBlock)

    newBlock.getPorts.foreach { case (portName, port) =>  // all other cases, elaborate in place
      elaboratePort(path + portName, newBlock, port)
    }

    val deps = newBlock match {
      case newBlock: wir.Generator =>
        newBlock.getDependencies.map { depPath =>
          ElaborateRecord.ParamValue(path.asIndirect ++ depPath)
        }
      case _ => Seq()
    }
    elaboratePending.addNode(ElaborateRecord.Block(path), deps)

    newBlock
  }

  // Given a link library at some path, expand it and return the instantiated block.
  protected def expandLink(path: DesignPath, link: wir.LinkLibrary): wir.Link = {
    val libraryPath = link.target

    val linkPb = library.getLink(libraryPath) match {
      case Errorable.Success(linkPb) => linkPb
      case Errorable.Error(err) =>
        import edgir.elem.elem
        errors += CompilerError.LibraryError(path, libraryPath, err)
        elem.Link()
    }

    val newLink = new wir.Link(linkPb)

    // Elaborate ports and parameters
    constProp.setValue(path.asIndirect + IndirectStep.Name, TextValue(path.toString))
    processParamDeclarations(path, newLink)
    linkParams.put(path, linkPb.params.keys.toSeq.map(IndirectStep.Element(_)))

    newLink.getPorts.foreach { case (portName, port) =>
      elaboratePort(path + portName, newLink, port)
    }

    // For link-side port arrays: set ALLOCATED -> ELEMENTS and allow it to expand later
    newLink.getPorts.collect { case (portName, port: wir.PortArray) =>
      require(!port.portsSet) // links can't have fixed array elts
      constProp.addDirectedEquality(
        path.asIndirect + portName + IndirectStep.Elements, path.asIndirect + portName + IndirectStep.Allocated,
          path, s"$portName (link array-from-connects)")
    }

    // Links can only elaborate when their port arrays are ready
    val arrayDeps = newLink.getPorts.collect {
      case (portName, arr: wir.PortArray) => ElaborateRecord.ElaboratePortArray(path + portName)
    }.toSeq
    elaboratePending.addNode(ElaborateRecord.Link(path), arrayDeps)

    newLink
  }

  // Expand an link array in-place.
  protected def expandLinkArray(path: DesignPath, array: wir.LinkArray): Unit = {
    val libraryPath = array.getModelLibrary

    val modelPb = library.getLink(libraryPath) match {
      case Errorable.Success(linkPb) =>
        linkPb
      case Errorable.Error(err) =>
        import edgir.elem.elem
        errors += CompilerError.LibraryError(path, libraryPath, err)
        elem.Link()
    }
    val model = new wir.Link(modelPb)
    array.createFrom(model)

    // For all arrays, size ELEMENTS directly from ALLOCATED
    model.getPorts.collect { case (portName, port: wir.PortArray) =>
      constProp.addDirectedEquality(
        path.asIndirect + portName + IndirectStep.Elements, path.asIndirect + portName + IndirectStep.Allocated,
        path, s"$portName (link-array array-from-connects)")
    }

    val arrayPortDeps = model.getPorts.collect { case (portName, port: wir.PortArray) =>
      ElaborateRecord.ParamValue(path.asIndirect + portName + IndirectStep.Elements)
    }.toSeq
    val elementDep = ElaborateRecord.ParamValue(path.asIndirect + IndirectStep.Elements)

    elaboratePending.addNode(ElaborateRecord.LinkArray(path), arrayPortDeps :+ elementDep)
  }

  protected def runGenerator(path: DesignPath, generator: wir.Generator): Unit = {
    val reqParamValues = generator.getDependencies.map { reqParam =>
      reqParam -> constProp.getValue(path.asIndirect ++ reqParam).get
    }

    // Run generator and plug in
    library.runGenerator(generator.getBlockClass, reqParamValues.toMap) match {
      case Errorable.Success(generatedPb) =>
        val generatedPorts = generator.applyGenerated(generatedPb)
        generatedPorts.foreach { portName =>
          val portArray = generator.getPorts(portName).asInstanceOf[wir.PortArray]
          constProp.setValue(path.asIndirect + portName + IndirectStep.Elements,
            ArrayValue(portArray.getPorts.keys.toSeq.map(TextValue(_))))
          // the rest was already handled when elaboratePorts on the generator stub
        }
      case Errorable.Error(err) =>
        errors += CompilerError.GeneratorError(path, generator.getBlockClass, err)
    }
  }

  /** Elaborate a block, mainly processing its internal blocks, links, and connected and parameter constraints.
    * The block should already have had its interface (ports and parameters) expanded.
    */
  protected def elaborateBlock(path: DesignPath): Unit = {
    val block = resolveBlock(path).asInstanceOf[wir.Block]

    block match {
      case block: wir.Generator => runGenerator(path, block)
      case _ =>  // ignored
    }

    // Queue up sub-trees that need elaboration - needs to be post-generate for generators
    block.getBlocks.foreach { case (innerBlockName, innerBlock) =>
      val innerBlockElaborated = expandBlock(path + innerBlockName, innerBlock.asInstanceOf[wir.BlockLibrary])
      block.elaborate(innerBlockName, innerBlockElaborated)
    }

    block.getLinks.foreach {
      case (innerLinkName, innerLink: wir.LinkLibrary) =>
        block.elaborate(innerLinkName, expandLink(path + innerLinkName, innerLink))
      case (innerLinkName, innerLink: wir.LinkArray) =>
        expandLinkArray(path + innerLinkName, innerLink)
      case _ => throw new NotImplementedError()
    }

    val connectedConstraints = new ConnectedConstraintManager(block)
    // Set IsConnected and generate constraint expansion records
    import edg.ExprBuilder.ValueExpr
    block.getBlocks.foreach { case (innerBlockName, innerBlock) =>
      innerBlock.asInstanceOf[wir.Block].getPorts.foreach { case (portName, port) =>
        val portPostfix = Seq(innerBlockName, portName)
        port match {
          case _: wir.PortArray =>  // array case: connectivity delayed to lowering
            connectedConstraints.connectionsByBlockPort(portPostfix) match {
              case PortConnections.ArrayConnect(constrName, constr) => constr.expr match {
                case expr.ValueExpr.Expr.ConnectedArray(connected) =>
                  val linkPortPostfix = connected.getLinkPort match {
                    case ValueExpr.Ref(linkPortPostfix) => linkPortPostfix
                    case ValueExpr.RefAllocate(linkPortPostfix, _) => linkPortPostfix
                    case _ => throw new IllegalArgumentException
                  }

                  constProp.addEquality(path.asIndirect ++ portPostfix + IndirectStep.Elements,
                    path.asIndirect + linkPortPostfix.head + IndirectStep.Elements)  // use link's ELEMENTS
                  constProp.addEquality(path.asIndirect ++ portPostfix + IndirectStep.Allocated,
                    path.asIndirect ++ portPostfix + IndirectStep.Elements)  // TODO can this be directed?

                  val expandArrayTask = ElaborateRecord.ExpandArrayConnections(path, constrName)
                  // Note: actual expansion task set on the link side
                  val resolveConnectedTask = ElaborateRecord.ResolveArrayIsConnected(path, portPostfix, Seq(), Seq(constrName), false)
                  elaboratePending.addNode(resolveConnectedTask, Seq(
                    ElaborateRecord.ElaboratePortArray(path ++ portPostfix),
                    expandArrayTask
                  ))

                case expr.ValueExpr.Expr.ExportedArray(exported) =>  // note internal port is portPostfix
                  val ValueExpr.Ref(extPostfix) = exported.getExteriorPort
                  constProp.addDirectedEquality(path.asIndirect ++ extPostfix + IndirectStep.Elements,
                    path.asIndirect ++ portPostfix + IndirectStep.Elements,
                    path, constrName)
                  constProp.addDirectedEquality(path.asIndirect ++ portPostfix + IndirectStep.Allocated,
                    path.asIndirect ++ extPostfix + IndirectStep.Allocated,
                    path, constrName)
                  val expandArrayTask = ElaborateRecord.ExpandArrayConnections(path, constrName)
                  elaboratePending.addNode(expandArrayTask,
                    Seq(
                      ElaborateRecord.ParamValue(path.asIndirect ++ portPostfix + IndirectStep.Elements),
                      // allocated must run first, it depends on constraints not being lowered
                      ElaborateRecord.ParamValue(path.asIndirect ++ portPostfix + IndirectStep.Allocated)
                  ))
                  val resolveConnectedTask = ElaborateRecord.ResolveArrayIsConnected(path, portPostfix, Seq(), Seq(constrName), false)
                  elaboratePending.addNode(resolveConnectedTask, Seq(
                    ElaborateRecord.ElaboratePortArray(path ++ portPostfix),
                    expandArrayTask))

                case _ => throw new IllegalArgumentException(s"invalid array connect to array $constr")
              }

              case PortConnections.AllocatedConnect(singleConnects, arrayConnects) =>
                val setAllocatedTask = ElaborateRecord.ResolveArrayAllocated(path, portPostfix, singleConnects.map(_._2), arrayConnects.map(_._2), false)
                elaboratePending.addNode(setAllocatedTask, Seq())
                val expandArrayConnectTasks = arrayConnects.map { case (allocated, constrName, constr) =>
                  ElaborateRecord.ExpandArrayConnections(path, constrName)
                }
                val resolveAllocateTask = ElaborateRecord.RewriteConnectAllocate(path, portPostfix, singleConnects.map(_._2), arrayConnects.map(_._2), false)
                elaboratePending.addNode(resolveAllocateTask,
                  Seq(ElaborateRecord.ElaboratePortArray(path ++ portPostfix)) ++ expandArrayConnectTasks :+ setAllocatedTask)
                val resolveConnectedTask = ElaborateRecord.ResolveArrayIsConnected(path, portPostfix, singleConnects.map(_._2), arrayConnects.map(_._2), false)
                elaboratePending.addNode(resolveConnectedTask, Seq(resolveAllocateTask))

              case PortConnections.NotConnected =>
                constProp.setValue(path.asIndirect ++ portPostfix + IndirectStep.Allocated, ArrayValue(Seq()))
                val resolveConnectedTask = ElaborateRecord.ResolveArrayIsConnected(path, portPostfix, Seq(), Seq(), false)
                elaboratePending.addNode(resolveConnectedTask, Seq(
                  ElaborateRecord.ElaboratePortArray(path ++ portPostfix)))

              case connects => throw new IllegalArgumentException(s"invalid connections to array $connects")
            }

          case _ =>  // leaf only, no array support
            connectedConstraints.connectionsByBlockPort(portPostfix) match {
              case PortConnections.SingleConnect(constrName, constr) =>
                resolvePortConnectivity(path, portPostfix, Some(constrName, constr))
              case PortConnections.NotConnected =>
                resolvePortConnectivity(path, portPostfix, None)
              case connects => throw new IllegalArgumentException(s"invalid connections to element $connects")
            }
        }
      }
    }

    block.getLinks.foreach {
      case (innerLinkName, innerLink: wir.Link) => innerLink.getPorts.foreach { case (portName, port) =>
        val portPostfix = Seq(innerLinkName, portName)
        port match {
          case _: wir.PortArray =>  // array case: connectivity delayed to lowering
            connectedConstraints.connectionsByLinkPort(portPostfix, false) match {
              case PortConnections.ArrayConnect(constrName, constr) =>
                throw new NotImplementedError()

              case PortConnections.AllocatedConnect(singleConnects, arrayConnects) =>
                require(arrayConnects.isEmpty)
                val setAllocatedTask = ElaborateRecord.ResolveArrayAllocated(path, portPostfix, singleConnects.map(_._2), Seq(), false)
                elaboratePending.addNode(setAllocatedTask, Seq())
                val resolveAllocateTask = ElaborateRecord.RewriteConnectAllocate(path, portPostfix, singleConnects.map(_._2), Seq(), false)
                elaboratePending.addNode(resolveAllocateTask,
                  Seq(ElaborateRecord.ElaboratePortArray(path ++ portPostfix)) :+ setAllocatedTask)
                val resolveConnectedTask = ElaborateRecord.ResolveArrayIsConnected(path, portPostfix, singleConnects.map(_._2), Seq(), false)
                elaboratePending.addNode(resolveConnectedTask, Seq(
                  resolveAllocateTask))

              case PortConnections.NotConnected =>
                constProp.setValue(path.asIndirect ++ portPostfix + IndirectStep.Allocated, ArrayValue(Seq()))
                val resolveConnectedTask = ElaborateRecord.ResolveArrayIsConnected(path, portPostfix, Seq(), Seq(), false)
                elaboratePending.addNode(resolveConnectedTask, Seq(
                  ElaborateRecord.ElaboratePortArray(path ++ portPostfix)))

              case connects => throw new IllegalArgumentException(s"invalid connections to array $connects")
            }

          case _ =>
            connectedConstraints.connectionsByLinkPort(portPostfix, false) match {
              case PortConnections.SingleConnect(constrName, constr) =>
                resolvePortConnectivity(path, portPostfix, Some(constrName, constr))
              case PortConnections.NotConnected =>
                resolvePortConnectivity(path, portPostfix, None)
              case connects => throw new IllegalArgumentException(s"invalid connections to element $connects")
            }
        }
      }
      case (innerLinkName, innerLink: wir.LinkArray) => innerLink.getModelPorts.foreach { case (portName, port) =>
        val portPostfix = Seq(innerLinkName, portName)
        port match {
          case _: wir.PortArray => // array case: connectivity delayed to lowering
            connectedConstraints.connectionsByLinkPort(portPostfix, false) match {
              case PortConnections.AllocatedConnect(singleConnects, arrayConnects) =>
                require(singleConnects.isEmpty)
                val setAllocatedTask = ElaborateRecord.ResolveArrayAllocated(path, portPostfix, Seq(), arrayConnects.map(_._2), false)
                elaboratePending.addNode(setAllocatedTask, Seq(
                  ElaborateRecord.ParamValue(path.asIndirect + portPostfix.head + IndirectStep.Elements)))
                val resolveAllocateTask = ElaborateRecord.RewriteArrayAllocate(path, portPostfix, Seq(), arrayConnects.map(_._2), false)
                elaboratePending.addNode(resolveAllocateTask, Seq(setAllocatedTask))
                val expandArrayTasks = arrayConnects.map { arrayConnect =>
                  val expandArrayTask = ElaborateRecord.ExpandArrayConnections(path, arrayConnect._2)
                  elaboratePending.addNode(expandArrayTask, Seq(
                    ElaborateRecord.ParamValue(path.asIndirect + portPostfix.head + IndirectStep.Elements),
                    resolveAllocateTask))
                  expandArrayTask
                }
                val resolveConnectedTask = ElaborateRecord.ResolveArrayIsConnected(path, portPostfix, Seq(), arrayConnects.map(_._2), false)
                elaboratePending.addNode(resolveConnectedTask,
                  Seq(ElaborateRecord.ElaboratePortArray(path ++ portPostfix)) ++
                  expandArrayTasks)

              case PortConnections.NotConnected =>  // TODO what are NC semantics for link array?
                constProp.setValue(path.asIndirect ++ portPostfix + IndirectStep.Allocated, ArrayValue(Seq()))
                val resolveConnectedTask = ElaborateRecord.ResolveArrayIsConnected(path, portPostfix, Seq(), Seq(), false)
                elaboratePending.addNode(resolveConnectedTask, Seq(
                  ElaborateRecord.ElaboratePortArray(path ++ portPostfix)))

              case connects => throw new IllegalArgumentException(s"invalid connections to array $connects")
            }
          case _ =>  // non-array, eg Port or Bundle
            connectedConstraints.connectionsByLinkPort(portPostfix, false) match {
              case PortConnections.ArrayConnect(constrName, constr) => constr.expr match {
                case expr.ValueExpr.Expr.ConnectedArray(connected) =>
                  val expandArrayTask = ElaborateRecord.ExpandArrayConnections(path, constrName)
                  elaboratePending.addNode(expandArrayTask, Seq(
                    ElaborateRecord.ParamValue(path.asIndirect + portPostfix.head + IndirectStep.Elements)
                  ))
                  // Note: actual expansion task set on the link side
                  val resolveConnectedTask = ElaborateRecord.ResolveArrayIsConnected(path, portPostfix, Seq(), Seq(constrName), false)
                  elaboratePending.addNode(resolveConnectedTask, Seq(
                    ElaborateRecord.ElaboratePortArray(path ++ portPostfix),
                    expandArrayTask))

                case connects => throw new IllegalArgumentException(s"invalid connections to array $connects")
              }

              case PortConnections.NotConnected =>
                val resolveConnectedTask = ElaborateRecord.ResolveArrayIsConnected(path, portPostfix, Seq(), Seq(), false)
                elaboratePending.addNode(resolveConnectedTask, Seq(
                  ElaborateRecord.ElaboratePortArray(path ++ portPostfix)))

              case connects => throw new IllegalArgumentException(s"invalid connections to element $connects")
            }
        }
      }
      case _ => throw new IllegalArgumentException
    }

    // Process all the process-able constraints: parameter constraints and non-allocate connected
    block.getConstraints.foreach { case (constrName, constr) =>
      processParamConstraint(path, constrName, constr, constr)
      processConnectedConstraint(path, constrName, constr, false)
    }
  }

  /** Elaborate the unelaborated link at path (but where the parent has been elaborated and is reachable from root),
    * and adds it to the parent and replaces the lib_elem proto entry with a placeholder unknown.
    * Adds children to the pending queue, and adds constraints to constProp.
    * Expands connects in the parent, as needed.
    */
  protected def elaborateLink(path: DesignPath): Unit = {
    import edg.ExprBuilder.{Ref, ValueExpr}
    val link = resolveLink(path).asInstanceOf[wir.Link]

    // Queue up sub-trees that need elaboration
    link.getLinks.foreach { case (innerLinkName, innerLink) =>
      val innerLinkElaborated = expandLink(path + innerLinkName, innerLink.asInstanceOf[wir.LinkLibrary])
      link.elaborate(innerLinkName, innerLinkElaborated)
    }

    def setConnectedLink(portPath: DesignPath, port: PortLike): Unit = (port: @unchecked) match {
      case _: wir.Port | _: wir.Bundle =>
        elaboratePending.setValue(ElaborateRecord.ConnectedLink(portPath), None)
        connectedLink.put(portPath, path)
      case port: wir.PortArray =>
        port.getPorts.foreach { case (subPortName, subPort) =>
          setConnectedLink(portPath + subPortName, subPort)
        }
    }
    for ((portName, port) <- link.getPorts) {
      setConnectedLink(path + portName, port)
    }

    // Aggregate by inner link ports
    val connectedConstraints = new ConnectedConstraintManager(link)

    link.getLinks.foreach { case (innerLinkName, innerLink) =>
      innerLink.asInstanceOf[wir.Link].getPorts.foreach { case (portName, port) =>
        val portPostfix = Seq(innerLinkName, portName)
        port match {
          case _: wir.PortArray => // array case: ignored, handled in lowering
            connectedConstraints.connectionsByLinkPort(portPostfix, true) match {
              case PortConnections.AllocatedConnect(singleConnects, arrayConnects) =>
                val deps = arrayConnects.map { case (allocated, constrName, constr) =>
                  val extPostfix = constr.getExportedArray.getExteriorPort match {
                    case ValueExpr.MapExtract(ValueExpr.Ref(extPostfix), Ref(_)) => extPostfix
                    case extPort => throw new IllegalArgumentException(s"unknown exported exterior $extPort")
                  }
                  ElaborateRecord.ParamValue(path.asIndirect ++ extPostfix + IndirectStep.Allocated)
                }
                arrayConnects.foreach { case (allocated, constrName, constr) =>
                  val extPostfix = constr.getExportedArray.getExteriorPort match {
                    case ValueExpr.MapExtract(ValueExpr.Ref(extPostfix), Ref(_)) => extPostfix
                    case extPort => throw new IllegalArgumentException(s"unknown exported exterior $extPort")
                  }
                  val ValueExpr.RefAllocate(intPostfix, _) = constr.getExportedArray.getInternalBlockPort
                  elaboratePending.addNode(ElaborateRecord.ExpandArrayConnections(path, constrName), Seq(
                    ElaborateRecord.ParamValue(path.asIndirect ++ extPostfix + IndirectStep.Elements),
                    // allocated must run first, it depends on constraints not being lowered
                    ElaborateRecord.ParamValue(path.asIndirect ++ intPostfix + IndirectStep.Allocated)
                  ))
                }

                val setAllocatedTask = ElaborateRecord.ResolveArrayAllocated(path, portPostfix, singleConnects.map(_._2), arrayConnects.map(_._2), true)
                elaboratePending.addNode(setAllocatedTask, deps)

                val resolveAllocateTask = ElaborateRecord.RewriteConnectAllocate(path, portPostfix, singleConnects.map(_._2), arrayConnects.map(_._2), true)
                elaboratePending.addNode(resolveAllocateTask, Seq(ElaborateRecord.ElaboratePortArray(path ++ portPostfix)))

                val resolveConnectedTask = ElaborateRecord.ResolveArrayIsConnected(path, portPostfix, singleConnects.map(_._2), arrayConnects.map(_._2), false)
                elaboratePending.addNode(resolveConnectedTask, Seq(
                  resolveAllocateTask))

              case PortConnections.NotConnected =>
                constProp.setValue(path.asIndirect ++ portPostfix + IndirectStep.Allocated, ArrayValue(Seq()))
                val resolveConnectedTask = ElaborateRecord.ResolveArrayIsConnected(path, portPostfix, Seq(), Seq(), false)
                elaboratePending.addNode(resolveConnectedTask, Seq(
                  ElaborateRecord.ElaboratePortArray(path ++ portPostfix)))

              case connects => throw new IllegalArgumentException(s"invalid connections to element $connects")
            }
          case _ =>  // everything else generated
            connectedConstraints.connectionsByLinkPort(portPostfix, true) match {
              case PortConnections.SingleConnect(constrName, constr) =>
                resolvePortConnectivity(path, portPostfix, Some(constrName, constr))
              case PortConnections.NotConnected =>
                resolvePortConnectivity(path, portPostfix, None)
              case connects => throw new IllegalArgumentException(s"invalid connections to element $connects")
            }
        }
      }
    }

    // Process constraints, as in the block case
    link.getConstraints.foreach { case (constrName, constr) =>
      processParamConstraint(path, constrName, constr, constr)
      processConnectedConstraint(path, constrName, constr, true)
    }
  }

  protected def elaborateLinkArray(path: DesignPath): Unit = {
    val link = resolve(path).asInstanceOf[wir.LinkArray]

    val linkElements = ArrayValue.ExtractText(
      constProp.getValue(path.asIndirect + IndirectStep.Elements).get)
    val linkPortArrayElements = link.getModelPorts.collect {
      case (portName, port: wir.PortArray) =>
        val portElements = ArrayValue.ExtractText(
          constProp.getValue(path.asIndirect + portName + IndirectStep.Elements).get)
        constProp.setValue(path.asIndirect + portName + IndirectStep.Length, IntValue(portElements.size))
        elaboratePending.setValue(ElaborateRecord.ElaboratePortArray(path + portName), None) // resolved in initPortsFromModel
        portName -> portElements
    }

    // Propagate link-wide ELEMENTS to port ELEMENTS and inner-link ALLOCATED
    link.getModelPorts.foreach {
      case (portName, port: wir.PortArray) =>
        linkPortArrayElements(portName).foreach { index =>
          constProp.addDirectedEquality(
            path.asIndirect + portName + index + IndirectStep.Elements,
            path.asIndirect + IndirectStep.Elements,
            path, s"$portName.$index (link-array ports-from-elts)")
        }
        linkElements.foreach { elementIndex =>
          constProp.addDirectedEquality(
            path.asIndirect + elementIndex + portName + IndirectStep.Allocated,
            path.asIndirect + portName + IndirectStep.Elements,
            path, s"$elementIndex.$portName (link-array inner-port-array from outer-elts)")
        }
      case (portName, port) =>
        constProp.addDirectedEquality(
          path.asIndirect + portName + IndirectStep.Elements,
          path.asIndirect + IndirectStep.Elements,
          path, s"$portName (link-array ports-from-elts)")
    }
    // Then expand the port-arrays
    link.initPortsFromModel(linkPortArrayElements).foreach { case (createdPortPostfix, createdPort) =>
      elaboratePortArray(path ++ createdPortPostfix)
      elaboratePending.setValue(ElaborateRecord.ElaboratePortArray(path ++ createdPortPostfix), None)
    }

    // Create internal links
    link.initLinks(linkElements).foreach { case (createdLinkName, createdLink) =>
      val innerLinkElaborated = expandLink(path + createdLinkName, createdLink)
      link.elaborate(createdLinkName, innerLinkElaborated)
    }

    // Create internal connects
    link.initConstraints(linkElements, linkPortArrayElements)

    // Resolve connections
    import edg.ExprBuilder.ValueExpr
    link.getConstraints.foreach { case (constrName, constr) =>
      processConnectedConstraint(path, constrName, constr, true)
    }

    // Resolve is-connected - need to sort by inner link's outermost port
    link.getConstraints.toSeq.map { case (constrName, constr) =>
      val ValueExpr.Ref(portPostfix) = constr.getExported.getInternalBlockPort
      link.getModelPorts(portPostfix(1)) match {
        case _: wir.PortArray =>
          (portPostfix.init, (constrName, constr)) // drop the array index
        case _ => // non-array like Port and Bundle
          (portPostfix, (constrName, constr))
      }
    }.groupBy(_._1).foreach { case (portPostfix, elts) =>  // actually resolve (delayed if array)
      val constrNamesConstrs = elts.map { _._2 }
      link.getModelPorts(portPostfix(1)) match {
        case _: wir.PortArray =>
          val constrNames = constrNamesConstrs.map { case (constrName, constr) => constrName }
          val resolveConnectedTask = ElaborateRecord.ResolveArrayIsConnected(path, portPostfix, constrNames, Seq(), false)
          elaboratePending.addNode(resolveConnectedTask, Seq(
            ElaborateRecord.ElaboratePortArray(path ++ portPostfix)))
        case _ => // non-array like Port and Bundle
          val Seq((constrName, constr)) = constrNamesConstrs  // can only be one element
          resolvePortConnectivity(path, portPostfix, Some(constrName, constr))
      }
    }
  }

  def elaboratePortArray(path: DesignPath): Unit = {
    val port = resolvePort(path).asInstanceOf[wir.PortArray]
    if (!port.portsSet) {
      val childPortNames = ArrayValue.ExtractText(constProp.getValue(path.asIndirect + IndirectStep.Elements).get)
      val childPortLibraries = SeqMap.from(childPortNames map { childPortName =>
        childPortName -> wir.PortLibrary.apply(port.getType)
      })
      port.setPorts(childPortLibraries)
    }
    for ((childPortName, childPort) <- port.getPorts) {
      elaboratePort(path + childPortName, port, childPort)
    }
    constProp.setValue(path.asIndirect + IndirectStep.Length, IntValue(port.getPorts.size))
  }

  // Once all array-connects have defined lengths, this lowers the array-connect statements by replacing them
  // with single leaf-level connections.
  protected def expandArrayConnections(record: ElaborateRecord.ExpandArrayConnections): Unit = {
    val parentBlock = resolve(record.parent).asInstanceOf[wir.HasMutableConstraints]  // can be block or link

    import edg.ExprBuilder.{Ref, ValueExpr}
    val newConstrNames = parentBlock.getConstraints(record.constraintName).expr match {
      case expr.ValueExpr.Expr.ExportedArray(exported) => (exported.getExteriorPort, exported.getInternalBlockPort) match {
        case (ValueExpr.Ref(extPortArray), ValueExpr.Ref(intPortArray)) =>
          val intPortArrayElts = ArrayValue.ExtractText(  // propagates inner to outer
            constProp.getValue(record.parent.asIndirect ++ intPortArray + IndirectStep.Elements).get)
          parentBlock.mapMultiConstraint(record.constraintName) { constr =>
            intPortArrayElts.map { index =>
              val newConstr = constr.asSingleConnection.connectUpdateRef { // tack an index on both sides
                case ValueExpr.Ref(ref) if ref == extPortArray => ValueExpr.Ref((ref :+ index): _*)
              }.connectUpdateRef {
                case ValueExpr.Ref(ref) if ref == intPortArray => ValueExpr.Ref((ref :+ index): _*)
              }
              s"${record.constraintName}.$index" -> newConstr
            }
          }.keys

        case (ValueExpr.MapExtract(ValueExpr.Ref(extPortArray), _), ValueExpr.RefAllocate(_, _)) =>
          val extPortArrayElts = ArrayValue.ExtractText(  // propagates outer to inner
            constProp.getValue(record.parent.asIndirect ++ extPortArray + IndirectStep.Elements).get)
          parentBlock.mapMultiConstraint(record.constraintName) { constr =>
            extPortArrayElts.map { index =>
              val newConstr = constr.asSingleConnection.connectUpdateRef {
                case ValueExpr.MapExtract(ValueExpr.Ref(extPortArray), Ref(extPortInner)) =>
                  ValueExpr.Ref((extPortArray ++ Seq(index) ++ extPortInner): _*)
                // inner side remains an allocate
              }
              s"${record.constraintName}.$index" -> newConstr
            }
          }.keys

        case _ => throw new IllegalArgumentException("unsupported array export")
      }
      case expr.ValueExpr.Expr.ConnectedArray(connected) =>
        // in all cases, the expansion is by link's elements, and any link-side allocations must be resolved
        val linkArrayPostfix = Seq(connected.getLinkPort.getRef.steps.head.getName)
        val linkArrayElts = ArrayValue.ExtractText( // propagates inner to outer
          constProp.getValue(record.parent.asIndirect ++ linkArrayPostfix + IndirectStep.Elements).get)
        parentBlock.mapMultiConstraint(record.constraintName) { constr =>
          linkArrayElts.map { index =>
            val newConstr = constr.asSingleConnection.connectUpdateRef { // tack an index on both sides
              case ValueExpr.Ref(ref) if !ref.startsWith(linkArrayPostfix) => ValueExpr.Ref((ref :+ index): _*)
              case ValueExpr.RefAllocate(ref, None) if !ref.startsWith(linkArrayPostfix) =>
                ValueExpr.RefAllocate(ref, None)  // allocate stays intact
              case ValueExpr.RefAllocate(ref, Some(suggestedName)) if !ref.startsWith(linkArrayPostfix) =>
                ValueExpr.RefAllocate(ref, Some(s"${suggestedName}_$index"))  // index tacked onto suggested name
            }.connectUpdateRef {
              case ValueExpr.Ref(ref) if ref.startsWith(linkArrayPostfix) => ValueExpr.Ref((ref :+ index): _*)
            }
            s"${record.constraintName}.$index" -> newConstr
          }
        }.keys

      case _ => throw new IllegalArgumentException
    }

    expandedArrayConnectConstraints.put(record.parent + record.constraintName, newConstrNames.toSeq)
    newConstrNames foreach { constrName =>
      // note no guarantee these are fully lowered, since the other side may have un-lowered allocates
      processConnectedConstraint(record.parent, constrName, parentBlock.getConstraints(constrName),
        parentBlock.isInstanceOf[wir.Link])
    }
  }

  // Sets the ALLOCATED on a port array, once all connections are of known length.
  // Array-connects must not have been lowered.
  protected def resolveArrayAllocated(record: ElaborateRecord.ResolveArrayAllocated): Unit = {
    val parentBlock = resolve(record.parent).asInstanceOf[wir.HasMutableConstraints]  // can be block or link

    import edg.ExprBuilder.{Ref, ValueExpr}
    val connectedIndices = record.constraintNames.map { constrName =>
      parentBlock.getConstraints(constrName).connectMapRef {
        case ValueExpr.RefAllocate(record.portPath, index) => index
      }
    }
    val arrayIndices = record.arrayConstraintNames.map { constrName =>
      parentBlock.getConstraints(constrName).expr
    }.flatMap {
      case expr.ValueExpr.Expr.ExportedArray(exported) => (exported.getExteriorPort, exported.getInternalBlockPort) match {
        case (ValueExpr.Ref(extPortArray), ValueExpr.Ref(record.portPath)) =>  // direct export, return external contents
          require(record.constraintNames.isEmpty && record.arrayConstraintNames.length == 1)  // non-allocating export only allowed once
          val elts = ArrayValue.ExtractText(
            constProp.getValue(record.parent.asIndirect ++ extPortArray + IndirectStep.Allocated).get)
          elts.map(Some(_))
        case (ValueExpr.MapExtract(ValueExpr.Ref(extPortArray), Ref(_)), ValueExpr.RefAllocate(record.portPath, None)) =>
          val elts = ArrayValue.ExtractText(
            constProp.getValue(record.parent.asIndirect ++ extPortArray + IndirectStep.Allocated).get)
          Seq.fill(elts.length)(None)
        case _ => throw new IllegalArgumentException
      }

      case expr.ValueExpr.Expr.ConnectedArray(connected) => (connected.getBlockPort, connected.getLinkPort) match {
        case (_, ValueExpr.RefAllocate(record.portPath, None)) =>  // link-side of array-connect treated as a unit
          Seq(None)

        case (ValueExpr.RefAllocate(record.portPath, suggestedName), ValueExpr.RefAllocate(linkPath, _)) =>
          val elts = ArrayValue.ExtractText(
            constProp.getValue(record.parent.asIndirect + linkPath.head + IndirectStep.Elements).get)
          suggestedName match {
            case Some(suggestedName) => elts.map { elt => Some(s"${suggestedName}_$elt") }
            case None => Seq.fill(elts.length)(None)
          }
          // TODO Ref and RefAllocate cases are completely duplicated
        case (ValueExpr.RefAllocate(record.portPath, suggestedName), ValueExpr.Ref(linkPath)) =>
          val elts = ArrayValue.ExtractText(
            constProp.getValue(record.parent.asIndirect + linkPath.head + IndirectStep.Elements).get)
          suggestedName match {
            case Some(suggestedName) => elts.map { elt => Some(s"${suggestedName}_$elt") }
            case None => Seq.fill(elts.length)(None)
          }

        case _ => throw new IllegalArgumentException
      }
      case _ => throw new IllegalArgumentException
    }

    val freeIndices = LazyList.from(0).iterator
    val allocatedIndices = (connectedIndices ++ arrayIndices).map {
      case Some(suggestedName) => suggestedName
      case None => freeIndices.next().toString
    }

    constProp.setValue(record.parent.asIndirect ++ record.portPath + IndirectStep.Allocated,
      ArrayValue(allocatedIndices.map(TextValue(_))))
  }

  // Once all connects to a link-array port are aggregated, rewrite the ALLOCATEs with concrete indices.
  protected def rewriteArrayAllocate(record: ElaborateRecord.RewriteArrayAllocate): Unit = {
    val parentBlock = resolve(record.parent).asInstanceOf[wir.Block]
    require(record.constraintNames.isEmpty)

    import edg.ExprBuilder.ValueExpr
    val freeNames = LazyList.from(0).iterator
    record.arrayConstraintNames.foreach { constrName =>
      parentBlock.mapConstraint(constrName) { constr => constr.arrayUpdateRef {
        case ValueExpr.RefAllocate(record.portPath, None) =>
          ValueExpr.Ref((record.portPath :+ freeNames.next().toString):_*)
      } }
    }
  }

  // Once a block-side port array has all its element widths available, this lowers the connect statements
  // by replacing ALLOCATEs with concrete indices.
  // This must also handle internal-side export statements.
  protected def rewriteConnectAllocate(record: ElaborateRecord.RewriteConnectAllocate): Unit = {
    val parentBlock = resolve(record.parent).asInstanceOf[wir.HasMutableConstraints]  // can be block or link
    val portElements = ArrayValue.ExtractText(
      constProp.getValue(record.parent.asIndirect ++ record.portPath + IndirectStep.Elements).get)
    // Update constraint names given expanded array constraints
    val combinedConstrNames = record.constraintNames ++
        record.arrayConstraintNames.flatMap(constrName => expandedArrayConnectConstraints(record.parent + constrName))

    import edg.ExprBuilder.ValueExpr
    val suggestedNames = combinedConstrNames.flatMap { constrName =>
      parentBlock.getConstraints(constrName).connectMapRef {
        case ValueExpr.RefAllocate(record.portPath, index) => index
      }
    }.toSet

    val freeNames = portElements.filter(!suggestedNames.contains(_)).iterator
    combinedConstrNames foreach { constrName =>
      parentBlock.mapConstraint(constrName) { constr => constr.connectUpdateRef {
        case ValueExpr.RefAllocate(record.portPath, Some(suggestedName)) =>
          require(portElements.contains(suggestedName), s"suggested name $suggestedName not in array $portElements")
          ValueExpr.Ref((record.portPath :+ suggestedName):_*)
        case ValueExpr.RefAllocate(record.portPath, None) =>
          ValueExpr.Ref((record.portPath :+ freeNames.next()):_*)
      } }
    }

    combinedConstrNames foreach { constrName =>
      // note no guarantee these are fully lowered, since the other side may have un-lowered allocates
      processConnectedConstraint(record.parent, constrName, parentBlock.getConstraints(constrName), record.portIsLink)
    }
  }

  protected def resolveArrayIsConnected(record: ElaborateRecord.ResolveArrayIsConnected): Unit = {
    val parentBlock = resolve(record.parent).asInstanceOf[wir.HasMutableConstraints]  // can be block or link
    val combinedConstrNames = record.constraintNames ++
        record.arrayConstraintNames.flatMap(constrName => expandedArrayConnectConstraints(record.parent + constrName))

    import edg.ExprBuilder.ValueExpr
    val allocatedIndexToConstraint = combinedConstrNames.map { constrName =>
      parentBlock.getConstraints(constrName).connectMapRef {
        case ValueExpr.Ref(record.portPath :+ index) => (Seq(index), constrName)
        case ValueExpr.Ref(record.portPath :+ index :+ interior) => (Seq(index, interior), constrName)  // for link arrays
      }
    }.toMap

    val portArray = resolve(record.parent ++ record.portPath).asInstanceOf[wir.PortArray]
    portArray.getPorts.foreach {
      case (index, innerPort: wir.PortArray) =>  // for link arrays
        require(innerPort.isElaborated)
        innerPort.getPorts.foreach { case (subIndex, subPort) =>
          val constraintOption = allocatedIndexToConstraint.get(Seq(index, subIndex)).map { constrName =>
            (constrName, parentBlock.getConstraints(constrName))
          }
          resolvePortConnectivity(record.parent, record.portPath :+ index :+ subIndex, constraintOption)
        }
      case (index, innerPort) =>
        val constraintOption = allocatedIndexToConstraint.get(Seq(index)).map { constrName =>
          (constrName, parentBlock.getConstraints(constrName))
        }
        resolvePortConnectivity(record.parent, record.portPath :+ index, constraintOption)
    }
  }

  /** Performs full compilation and returns the resulting design. Might take a while.
    */
  def compile(): schema.Design = {
    import edg.ElemBuilder

    while (elaboratePending.getReady.nonEmpty) {
      elaboratePending.getReady.foreach { elaborateRecord =>
        onElaborate(elaborateRecord)
        elaborateRecord match {
          case elaborateRecord@ElaborateRecord.Block(blockPath) =>
            elaborateBlock(blockPath)
            elaboratePending.setValue(elaborateRecord, None)
          case elaborateRecord@ElaborateRecord.Link(linkPath) =>
            elaborateLink(linkPath)
            elaboratePending.setValue(elaborateRecord, None)
          case elaborateRecord@ElaborateRecord.LinkArray(linkPath) =>
            elaborateLinkArray(linkPath)
            elaboratePending.setValue(elaborateRecord, None)
          case connect: ElaborateRecord.Connect =>
            elaborateConnect(connect)
            elaboratePending.setValue(elaborateRecord, None)

          case elaborateRecord: ElaborateRecord.ElaboratePortArray =>
            elaboratePortArray(elaborateRecord.path)
            elaboratePending.setValue(elaborateRecord, None)

          case elaborateRecord: ElaborateRecord.ExpandArrayConnections =>
            expandArrayConnections(elaborateRecord)
            elaboratePending.setValue(elaborateRecord, None)
          case elaborateRecord: ElaborateRecord.RewriteArrayAllocate =>
            rewriteArrayAllocate(elaborateRecord)
            elaboratePending.setValue(elaborateRecord, None)
          case elaborateRecord: ElaborateRecord.ResolveArrayAllocated =>
            resolveArrayAllocated(elaborateRecord)
            elaboratePending.setValue(elaborateRecord, None)
          case elaborateRecord: ElaborateRecord.RewriteConnectAllocate =>
            rewriteConnectAllocate(elaborateRecord)
            elaboratePending.setValue(elaborateRecord, None)
          case elaborateRecord: ElaborateRecord.ResolveArrayIsConnected =>
            resolveArrayIsConnected(elaborateRecord)
            elaboratePending.setValue(elaborateRecord, None)

          case _: ElaborateRecord.ElaborateDependency =>
            throw new IllegalArgumentException(s"can't elaborate dependency-only record $elaborateRecord")
        }
      }
    }

    ElemBuilder.Design(root.toPb)
  }

  def evaluateExpr(root: DesignPath, value: expr.ValueExpr): ExprResult = {
    new ExprEvaluatePartial(constProp, root).map(value)
  }

  def getParamValue(param: IndirectDesignPath): Option[ExprValue] = constProp.getValue(param)
  def getAllSolved: Map[IndirectDesignPath, ExprValue] = constProp.getAllSolved
  def getConnectedLink(port: DesignPath): Option[DesignPath] = connectedLink.get(port)
}
