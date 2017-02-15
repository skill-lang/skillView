package qq.graph

import de.ust.skill.common.scala.api
import de.ust.skill.common.scala.internal.fieldTypes._

/** the thing a link in the graph represents */
abstract class AbstractEdge {
  def getTo: AbstractNode
  def getFrom: AbstractNode
  def textLabel: String
}

case class SkillFieldEdge[T](
    val from: api.SkillObject,
    val field: api.FieldDeclaration[T])
    extends AbstractEdge {
  
  val to = from.get(field)
  
  override def hashCode = 17 + 31 * java.util.Objects.hash(from, field) + to.hashCode()
  override def equals(that: Any) = that match {
    case that: SkillFieldEdge[T] =>
      from == that.from && field == that.field && to == that.to
    case _ => false
  }
  
  override def getFrom = new SkillObjectNode(from)
  override def getTo = field.t.asInstanceOf[FieldType[_]] match {
    case u: UserType[t] => if (to != null) new SkillObjectNode(to.asInstanceOf[t]) else new NullNode(from, field)
    case a: AnnotationType => if (to != null) new SkillObjectNode(to.asInstanceOf[api.SkillObject]) else new NullNode(from, field)
    case I8| I16| I32| I64| V64| F32| F64| _:StringType| BoolType =>
      new ValueNode(from, field)
      
    
  }
  override def textLabel = field.name
  
 }
  

