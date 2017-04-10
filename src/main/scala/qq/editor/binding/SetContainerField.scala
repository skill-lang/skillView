package qq.editor.binding

import de.ust.skill.common.scala.api;
import de.ust.skill.common.scala.internal.fieldTypes.SingleBaseTypeContainer;
import de.ust.skill.common.scala.internal.fieldTypes.FieldType;
import scala.collection.mutable.HashSet;

/** Property for an element of a set field for use by edit components.
 *  Generates undoable UserEdit to update the file and monitors Edits to update its own state */
class SetContainerField[O <: api.SkillObject, C[F] <: HashSet[F], F](
  owner0: qq.util.binding.PropertyOwner,
  val file: qq.editor.File,
  val pool: api.Access[O],
  val obj: O,
  val field: api.FieldDeclaration[C[F]],
  val key: F)
    extends SkillFieldProperty[F](owner0, "", key) {

  def groundType = field.t.asInstanceOf[SingleBaseTypeContainer[C[F],F]].groundType
  
  description = s"""$groundType ${field.name}(${key match {
                        case o: api.SkillObject => file.idOfObj(o)
                        case x => x
                      }}) in ${file.idOfObj(obj)}"""
  
  var key_ = key
  restrictions ++= Restrictions(field)
  restrictions ++= Restrictions(file, field.t.asInstanceOf[SingleBaseTypeContainer[_,_]].groundType.asInstanceOf[FieldType[F]]) 
  restrictions += qq.util.binding.Restriction(x => x == this() || !obj.get(field).contains(x), "New value is already contained in set")
  
  /**
   * when obj.get(field)(index) is the last element and is removed, this object
   *  disables itself so that it can do no harm. Its owner will remove it.
   */
  private var disabled = false

  private val fileEditHandler: (qq.editor.Edit[_] ⇒ Unit) = { x ⇒
    if (!disabled && x.obj == obj && x.isInstanceOf[qq.editor.SetEdit[_, _, _]]) {
      val y = x.asInstanceOf[qq.editor.SetEdit[O, C[F], F]]
      if (y.field == field) {
        y match {
          case del: qq.editor.SetRemove[O, C[F], F] ⇒
            if (del.key == key_) {
                disabled = true
            }
          case mod: qq.editor.SetReplace[O, C[F], F] ⇒
            if (mod.key == key_) {
              println(s"repl $key_ ${mod.replacement}")
              key_ = mod.replacement
              this.assignUnchecked(mod.replacement)
            }
          case _: qq.editor.SetInsert[O, C[F], F] => ()
        }
      }
    }
  }
  file.onEdit.weak += fileEditHandler

  private def selfChangeHandler(x: F): Unit = {
              println(s"sch $key_ $x")
    if (key_ != x) new qq.editor.UserSetReplace(file, pool, obj, field, key_, x)
  }
  onChange.strong += selfChangeHandler

}