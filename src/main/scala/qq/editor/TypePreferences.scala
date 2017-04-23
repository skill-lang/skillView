package qq.editor

import de.ust.skill.common.scala.api;
import scala.collection.mutable;
/** Preferences for a skill type.
 *  
 *  Contains field preferences and the paths to expanded nodes.
 *  
 * */
class TypePreferences[T <: api.SkillObject](
  /** the skill type this is about */
  val typ: api.Access[T],
  /** the file this belongs to*/
  val containingFile: File
) {
  
  /**
   * Preferences for the fields in this type.
   * 
   * only own fields; behaviour of inherited fields is taken from parent
   */
  val fields: Map[api.FieldDeclaration[_], qq.editor.FieldPreferences[_,T]] =
    (for (f <- typ.fields) yield (f, new FieldPreferences(f, this))).toMap
  
  /** convenience method: get the preferences for the parent type */
  def parentTypePreferences: TypePreferences[_] = containingFile.typePreferences(
      containingFile.parentType(typ)
      )
      
  /** Sequences of fields that lead to expanded nodes when an object of this type is the main node. */
  val expanded: mutable.HashSet[Seq[api.FieldDeclaration[_]]] = mutable.HashSet()
  
  /** Not a preference :) – true if this type was deleted when the file was saved. */
  var isDeleted = false
}