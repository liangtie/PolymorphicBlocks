package edg.compiler

import scala.collection.mutable


// Base trait for expression values in edgir, should be consistent with init.proto and lit.proto
sealed trait ExprValue

// These should be consistent with what is in init.proto
object FloatPromotable {
  def unapply(floatPromotable: FloatPromotable): Option[Float] = {
    Some(floatPromotable.toFloat)
  }
}

sealed trait FloatPromotable extends ExprValue {
  def toFloat: Float
}

case class FloatValue(value: Float) extends FloatPromotable {
  override def toFloat: Float = value
}

case class IntValue(value: BigInt) extends FloatPromotable {
  override def toFloat: Float = value.toFloat  // note: potential loss of precision
}

object RangeValue {
  def empty: RangeValue = RangeValue(Float.NaN, Float.NaN)  // TODO proper null interval construct
  def isEmpty(value: RangeValue): Boolean = value.lower.isNaN && value.upper.isNaN
}

case class RangeValue(lower: Float, upper: Float) extends ExprValue {
  // TODO better definition of empty range
  require(lower <= upper || (lower.isNaN && upper.isNaN))
}
case class BooleanValue(value: Boolean) extends ExprValue
case class TextValue(value: String) extends ExprValue

object ArrayValue {
  /** Maps a Seq using a PartialFunction. Returns the output map if all elements were mapped, otherwise returns None.
    * May short circuit evaluate on the first PartialFunction failure.
    */
  protected def seqMapOption[InType, OutType](seq: Seq[InType])(mapPartialFunc: PartialFunction[InType, OutType]):
  Option[Seq[OutType]] = {
    // TODO is this actually more performant than doing a collect, even assuming that most of the time this will fail?
    val builder = mutable.Buffer[OutType]()
    val mapLifted = mapPartialFunc.lift
    for (elt <- seq) {
      mapLifted(elt) match {
        case Some(eltMapped) => builder += eltMapped
        case None => return None
      }
    }
    Some(builder.toList)
  }

  def unapply[T <: ExprValue](vals: ArrayValue[T]): Option[Seq[T]] = {
    Some(vals.values)
  }

  object ExtractFloat {
    def unapply[T <: ExprValue](vals: ArrayValue[T]): Option[Seq[Float]] = seqMapOption(vals.values) {
      case FloatValue(elt) => elt
    }
  }

  object ExtractInt {
    def unapply[T <: ExprValue](vals: ArrayValue[T]): Option[Seq[BigInt]] = seqMapOption(vals.values) {
      case IntValue(elt) => elt
    }
  }

  object ExtractRange {
    def unapply[T <: ExprValue](vals: ArrayValue[T]): Option[(Seq[Float], Seq[Float])] = seqMapOption(vals.values) {
      case RangeValue(eltMin, eltMax) => (eltMin, eltMax)
    }.map(_.unzip)
  }

  object ExtractBoolean {
    def unapply[T <: ExprValue](vals: ArrayValue[T]): Option[Seq[Boolean]] = seqMapOption(vals.values) {
      case BooleanValue(elt) => elt
    }
  }

  object ExtractText {
    def unapply[T <: ExprValue](vals: ArrayValue[T]): Option[Seq[String]] = seqMapOption(vals.values) {
      case TextValue(elt) => elt
    }
  }
}
case class ArrayValue[T <: ExprValue](values: Seq[T]) extends ExprValue