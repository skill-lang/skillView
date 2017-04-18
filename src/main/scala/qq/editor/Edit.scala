package qq.editor

import de.ust.skill.common.scala.api;
import de.ust.skill.common.scala.internal;
import scala.collection.mutable.Buffer;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.ListBuffer;
import scala.collection.mutable.HashSet;
import scala.collection.mutable.HashMap;
import javax.swing.undo._;

/**
 * Represents a modification of a skill file.
 * 
 * Instances of this class are used to notify the UI elements that they have to update.
 * Edits can not be undone, rather, an undo is an edit itself.
 * 
 * @see qq.editor.UserEdit an undoable modification that was requested by the user 
 */
sealed abstract class Edit[T <: api.SkillObject](
    /** The file that is modified */
    val file: qq.editor.File,
    /** The type of the modified object*/
    val pool: api.Access[T],
    /** The object that is modified */
    val obj: T) {

  /** Apply the change to [[Edit.file]].
   *  
   *  Do not call directly, pass object to file.modify, instead,
   *  in order to ensure that everything is notified about the update.
   *  
   *  Probably should be `protected` somehow */
  def doIt(): Unit
}

/** Creation of object `o` */
final case class CreateObject[T <: api.SkillObject](
  /** The file this belongs to */
  f: qq.editor.File,
  /** The type of the created object*/
  p: api.Access[T],
  /** The object that is modified */
  o: T)
    extends Edit[T](f, p, o) {

  override def doIt() = {
    /* remove from deleted if this creation undoes a deletion */
    file.deletedObjects -= obj
  }

}

/** Deletion of object `o` */
final case class DeleteObject[T <: api.SkillObject](
  /** The file this belongs to */
  f: qq.editor.File,
  /** The type of the deleted object*/
  p: api.Access[T],
  /** The object that is deleted */
  o: T)
    extends Edit[T](f, p, o) {

  override def doIt() = {
    file.deletedObjects += obj
  }

}

/** Modification of the value of a simple field */
final case class SimpleFieldEdit[T <: api.SkillObject, F](
  /** The file this belongs to */
  f: qq.editor.File,
  /** The type of the modified object*/
  p: api.Access[T],
  /** The object that is modified */
  o: T,
  /** The field that is modified */
  val field: api.FieldDeclaration[F],
  /** Original value of the field */
  val oldValue: F,
  /** Value after modification */
  val newValue: F)
    extends Edit[T](f, p, o) {

  override def doIt() = {
    obj.set(field, newValue)
  }
}
/** Modifications of things like arrays and lists that have indexed objects */
sealed abstract class IndexedContainerEdit[T <: api.SkillObject, C <: Iterable[F], F](
  /** The file this belongs to */
  f: qq.editor.File,
  /** The type of the modified object*/
  p: api.Access[T],
  /** The object that is modified */
  o: T,
  /** The field (collection) that is modified */
  val field: api.FieldDeclaration[C],
  /** The index of the modified member of the collection */
  val index: Int)
    extends Edit[T](f, p, o) {

}

/** insertion of a new value into an indexed container */
final case class IndexedContainerInsert[T <: api.SkillObject, C <: Buffer[F], F](
  /** The file this belongs to */
  f: qq.editor.File,
  /** The type of the modified object*/
  p: api.Access[T],
  /** The object that is modified */
  o: T,
  /** The field (collection) that is modified */
  fd: api.FieldDeclaration[C],
  /** The index of the modified member of the collection */
  i: Int,
  /** the value of the new member (the new f[i]; the old f[i] becomes f[i+1] &c.)*/
  val value: F)
    extends IndexedContainerEdit[T, C, F](f, p, o, fd, i) {

  override def doIt(): Unit = {
    obj.get(field).insert(index, value)
  }
}

/** removal of an value from an indexed container */
final case class IndexedContainerRemove[T <: api.SkillObject, C <: Buffer[F], F](
  /** The file this belongs to */
  f: qq.editor.File,
  /** The type of the modified object*/
  p: api.Access[T],
  /** The object that is modified */
  o: T,
  /** The field (collection) that is modified */
  fd: api.FieldDeclaration[C],
  /** The index of the modified member of the collection */
  i: Int,
  /** the value of the deleted member (the old f[i]; the new f[i] was former f[i+1] &c.)*/
  val value: F)
    extends IndexedContainerEdit[T, C, F](f, p, o, fd, i) {

  override def doIt(): Unit = {
    obj.get(field).remove(index)
  }
}

/** change of the value of a member of an indexed container */
final case class IndexedContainerModify[T <: api.SkillObject, C <: Buffer[F], F](
  /** The file this belongs to */
  f: qq.editor.File,
  /** The type of the modified object*/
  p: api.Access[T],
  /** The object that is modified */
  o: T,
  /** The field (collection) that is modified */
  fd: api.FieldDeclaration[C],
  /** The index of the modified member of the collection */
  i: Int,
  /** Value before modification */
  val oldValue: F,
  /** Value after modification */
  val newValue: F)
    extends IndexedContainerEdit[T, C, F](f, p, o, fd, i) {

  override def doIt(): Unit = {
    obj.get(field)(index) = newValue
  }
}

/** edits of sets */
sealed abstract class SetEdit[T <: api.SkillObject, C <: HashSet[F], F](
  /** The file this belongs to */
  f: qq.editor.File,
  /** The type of the modified object*/
  p: api.Access[T],
  /** The object that is modified */
  o: T,
  /** The field (set) that is modified */
  val field: api.FieldDeclaration[C],
  /** The modified element */
  val key: F)
    extends Edit[T](f, p, o) {

}

/** insertion of a new element into a set*/
final case class SetInsert[T <: api.SkillObject, C <: HashSet[F], F](
  /** The file this belongs to */
  f: qq.editor.File,
  /** The type of the modified object*/
  p: api.Access[T],
  /** The object that is modified */
  o: T,
  /** The field (set) that is modified */
  fd: api.FieldDeclaration[C],
  /** The new member */
  k: F)
    extends SetEdit[T, C, F](f, p, o, fd, k) {

  override def doIt(): Unit = {
    obj.get(field) += key
  }

}

/** removal of an element from a set */
final case class SetRemove[T <: api.SkillObject, C <: HashSet[F], F](
  /** The file this belongs to */
  f: qq.editor.File,
  /** The type of the modified object*/
  p: api.Access[T],
  /** The object that is modified */
  o: T,
  /** The field (set) that is modified */
  fd: api.FieldDeclaration[C],
  /** The removed member */
  k: F)
    extends SetEdit[T, C, F](f, p, o, fd, k) {

  override def doIt(): Unit = {
    obj.get(field) -= key
  }

}

/** replace an element (remove+insert) in a set */
final case class SetReplace[T <: api.SkillObject, C <: HashSet[F], F](
  /** The file this belongs to */
  f: qq.editor.File,
  /** The type of the modified object*/
  p: api.Access[T],
  /** The object that is modified */
  o: T,
  /** The field (collection) that is modified */
  fd: api.FieldDeclaration[C],
  /** The element that is replaced */
  k: F,
  /** the new value it is replaced with */
  val replacement: F)
    extends SetEdit[T, C, F](f, p, o, fd, k) {

  override def doIt(): Unit = {
    obj.get(field) -= key
    obj.get(field) += replacement
  }

}


/** maps are treated as a function from a key-tuple to value. We give up using scala types, here */
sealed abstract class MapEdit[T <: api.SkillObject, F](
  /** The file this belongs to */
  f: qq.editor.File,
  /** The type of the modified object*/
  p: api.Access[T],
  /** The object that is modified */
  o: T,
  /** The field map that is modified */
  val field: api.FieldDeclaration[F],
  /** The index as sequence of keys of the modified member of the collection */
  val index: Seq[Any])
    extends Edit[T](f, p, o) {

}
import de.ust.skill.common.scala.internal.fieldTypes.MapType

/** insertion of a new value into an indexed container */
final case class MapInsert[T <: api.SkillObject, F](
  /** The file this belongs to */
  f: qq.editor.File,
  /** The type of the modified object*/
  p: api.Access[T],
  /** The object that is modified */
  o: T,
  /** The field (collection) that is modified */
  fd: api.FieldDeclaration[F],
  /** The index of the modified member of the collection */
  i: Seq[Any],
  /** the value of the new member (the new f[i]; the old f[i] becomes f[i+1] &c.)*/
  val value: Any)
    extends MapEdit[T, F](f, p, o, fd, i) {

  override def doIt(): Unit = {
    val temp = obj.get(field)
    qq.util.FlattenedMap.insert(obj.get(field).asInstanceOf[HashMap[Any,Any]], fd.t.asInstanceOf[MapType[_,_]], index, value)
  }
}


/** removal of an value from an indexed container */
final case class MapRemove[T <: api.SkillObject, F](
  /** The file this belongs to */
  f: qq.editor.File,
  /** The type of the modified object*/
  p: api.Access[T],
  /** The object that is modified */
  o: T,
  /** The field (collection) that is modified */
  fd: api.FieldDeclaration[F],
  /** The index of the modified member of the collection */
  i: Seq[Any],
  /** the value of the new member (the new f[i]; the old f[i] becomes f[i+1] &c.)*/
  val value: Any)
    extends MapEdit[T, F](f, p, o, fd, i) {

  override def doIt(): Unit = {
    val temp = obj.get(field)
    qq.util.FlattenedMap.remove(obj.get(field).asInstanceOf[HashMap[Any,Any]], fd.t.asInstanceOf[MapType[_,_]], index)
  }
}

/** change of the value of a member of an indexed container */
final case class MapModify[T <: api.SkillObject, F](
  /** The file this belongs to */
  f: qq.editor.File,
  /** The type of the modified object*/
  p: api.Access[T],
  /** The object that is modified */
  o: T,
  /** The field (collection) that is modified */
  fd: api.FieldDeclaration[F],
  /** The index of the modified member of the collection */
  i: Seq[Any],
  /** Value before modification */
  val oldValue: Any,
  /** Value after modification */
  val newValue: Any)
    extends MapEdit[T, F](f, p, o, fd, i) {

  override def doIt(): Unit = {
    val temp = obj.get(field)
    qq.util.FlattenedMap.set(temp.asInstanceOf[HashMap[Any,Any]], fd.t.asInstanceOf[MapType[_,_]], index, newValue)
  }
}
