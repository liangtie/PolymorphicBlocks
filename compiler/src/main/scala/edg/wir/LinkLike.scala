package edg.wir

import edg.EdgirUtils.SimpleLibraryPath
import edgir.init.init
import edgir.elem.elem
import edgir.expr.expr
import edgir.ref.ref

import scala.collection.{SeqMap, mutable}


sealed trait LinkLike extends Pathable {
  def toPb: elem.LinkLike
}

/**
  * Similar to Block, see documentation there.
  */
class Link(pb: elem.Link) extends LinkLike
    with HasMutablePorts with HasMutableLinks with HasMutableConstraints with HasParams {
  private val nameOrder = ProtoUtil.getNameOrder(pb.meta)
  override protected val ports: mutable.SeqMap[String, PortLike] = parsePorts(pb.ports, nameOrder)
  override protected val links: mutable.SeqMap[String, LinkLike] = parseLinks(pb.links, nameOrder)
  override protected val constraints: mutable.SeqMap[String, expr.ValueExpr] = parseConstraints(pb.constraints, nameOrder)

  override def isElaborated: Boolean = true

  override def getParams: Map[String, init.ValInit] = pb.params

  override def resolve(suffix: Seq[String]): Pathable = suffix match {
    case Seq() => this
    case Seq(subname, tail@_*) =>
      if (ports.contains(subname)) {
        ports(subname).resolve(tail)
      } else if (links.contains(subname)) {
        links(subname).resolve(tail)
      } else {
        throw new InvalidPathException(s"No element $subname (of $suffix} in Link ${pb.getSelfClass.toSimpleString}")
      }
  }

  def toEltPb: elem.Link = {
    pb.copy(
      ports=ports.view.mapValues(_.toPb).toMap,
      links=links.view.mapValues(_.toPb).toMap,
      constraints=constraints.toMap
    )
  }

  override def toPb: elem.LinkLike = {
    elem.LinkLike(`type`=elem.LinkLike.Type.Link(toEltPb))
  }
}

class LinkArray(pb: elem.LinkArray) extends LinkLike
    with HasMutablePorts with HasMutableLinks with HasMutableConstraints {
  require(pb.ports.isEmpty && pb.links.isEmpty && pb.constraints.isEmpty, "link array may not start elaborated")
  override protected val ports = mutable.SeqMap[String, PortLike]()
  override protected val links = mutable.SeqMap[String, LinkLike]()
  override protected val constraints = mutable.SeqMap[String, expr.ValueExpr]()

  var model: Option[Link] = None

  def getModelLibrary: ref.LibraryPath = pb.getSelfClass

  def createFrom(linkDef: Link): Unit = {
    require(model.isEmpty)
    model = Some(linkDef)
  }

  def getModelPorts: Map[String, PortLike] = model.get.getPorts

  // Creates my ports from the model type and array lengths, returning the port postfix and created port.
  // Outer arrays have their elements set, while inner arrays (corresponding to the link-array's ELEMENTS)
  // must be set externally.
  def initPortsFromModel(arrayElements: Map[String, Seq[String]]): Map[Seq[String], PortArray] = {
    require(ports.isEmpty)
    model.get.getPorts.flatMap {
      case (portName, port: PortArray) =>
        val outerCreated = new PortArray(elem.PortArray(selfClass=None))
        ports.put(portName, outerCreated)
        val inner = arrayElements(portName).map { index =>
          val created = new PortArray(elem.PortArray(selfClass=Some(port.getType)))
          (index, created)
        }
        outerCreated.setPorts(inner.to(SeqMap))
        inner.map { case (index, created) => (Seq(portName, index), created) }
      case (portName, port: PortLibrary) =>
        val created = new PortArray(elem.PortArray(selfClass=Some(port.target)))
        ports.put(portName, created)
        Seq((Seq(portName), created))
      case _ => throw new IllegalArgumentException
    }
  }

  // Creates the specified amount of internal links, returning the name and created link.
  def initLinks(linkElements: Seq[String]): Map[String, LinkLibrary] = {
    require(links.isEmpty)
    linkElements.map { index =>
      val created = new LinkLibrary(getModelLibrary)
      links.put(index, created)
      (index, created)
    }.toMap
  }

  def initConstraints(linkElements: Seq[String], arrayElements: Map[String, Seq[String]]): Unit = {
    import edg.ElemBuilder.Constraint
    import edg.ExprBuilder.Ref
    require(constraints.isEmpty)
    model.get.getPorts.foreach {
      case (portName, port: PortArray) =>
        arrayElements(portName).foreach { arrayIndex =>
          linkElements.foreach { elementIndex =>
            constraints.put(s"$portName.$arrayIndex.$elementIndex", Constraint.Exported(
              Ref(portName, arrayIndex, elementIndex), Ref(elementIndex, portName, arrayIndex)
            ))
          }
        }
      case (portName, port: PortLibrary) =>
        linkElements.foreach { elementIndex =>
          constraints.put(s"$portName.$elementIndex", Constraint.Exported(
            Ref(portName, elementIndex), Ref(elementIndex, portName)
          ))
        }
      case _ => throw new IllegalArgumentException
    }
  }

  // TODO explicit isElaborated flag? instead of inferring?
  override def isElaborated: Boolean = !(pb.ports.isEmpty && pb.links.isEmpty && pb.constraints.isEmpty)

  override def resolve(suffix: Seq[String]): Pathable = suffix match {
    case Seq() => this
    case Seq(subname, tail@_*) =>
      if (ports.contains(subname)) {
        ports(subname).resolve(tail)
      } else if (links.contains(subname)) {
        links(subname).resolve(tail)
      } else {
        throw new InvalidPathException(s"No element $subname (of $suffix) in Link ${pb.getSelfClass.toSimpleString}")
      }
  }

  def toEltPb: elem.LinkArray = {
    pb.copy(
      ports=ports.view.mapValues(_.toPb).toMap,
      links=links.view.mapValues(_.toPb).toMap,
      constraints=constraints.toMap
    )
  }

  override def toPb: elem.LinkLike = {
    elem.LinkLike(`type`=elem.LinkLike.Type.Array(toEltPb))
  }
}

case class LinkLibrary(target: ref.LibraryPath) extends LinkLike {
  def resolve(suffix: Seq[String]): Pathable = suffix match {
    case Seq() => this
    case _ => throw new InvalidPathException(s"Can't resolve $suffix into library ${target.toSimpleString}")
  }
  def toPb: elem.LinkLike = elem.LinkLike(elem.LinkLike.Type.LibElem(target))
  override def isElaborated: Boolean = false
}
