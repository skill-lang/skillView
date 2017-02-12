/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 18.09.2016                               *
 * |___/_|\_\_|_|____|    by: m                                               *
\*                                                                            */
package empty.api.internal

import java.nio.file.Path

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.WeakHashMap
import scala.collection.mutable.HashSet

import de.ust.skill.common.jvm.streams.MappedInStream
import de.ust.skill.common.scala.api.SkillObject
import de.ust.skill.common.scala.api.TypeSystemError
import de.ust.skill.common.scala.api.WriteMode
import de.ust.skill.common.scala.internal.BasePool
import de.ust.skill.common.scala.internal.SkillFileParser
import de.ust.skill.common.scala.internal.StoragePool
import de.ust.skill.common.scala.internal.StringPool
import de.ust.skill.common.scala.internal.UnknownBasePool
import de.ust.skill.common.scala.internal.fieldTypes.AnnotationType
import de.ust.skill.common.scala.internal.restrictions.TypeRestriction

import _root_.empty.api.SkillFile

/**
 * Parametrization of the common skill parser.
 *
 * @author Timm Felden
 */
object FileParser extends SkillFileParser[SkillFile] {

  //mr: hack for keeping restrictions accessible (should go to storage pool, but …)
  val typeRestrictions: WeakHashMap[StoragePool[_, _], HashSet[TypeRestriction]] = WeakHashMap()

  // TODO we can make this faster using a hash map (for large type systems)
  def newPool(
    typeId: Int,
    name: String,
    superPool: StoragePool[_ <: SkillObject, _ <: SkillObject],
    rest: HashSet[TypeRestriction]): StoragePool[_ <: SkillObject, _ <: SkillObject] = {
    val result: StoragePool[_ <: SkillObject, _ <: SkillObject] = name match {
      case _ ⇒
        if (null == superPool)
          new UnknownBasePool(name, typeId)
        else
          superPool.makeSubPool(name, typeId)
    }
    println(name + " " + rest)
    //mr: hack for keeping restrictions accessible (should go to storage pool, but …)
    typeRestrictions(result) = rest
    result
  }

  def makeState(path: Path,
                mode: WriteMode,
                String: StringPool,
                Annotation: AnnotationType,
                types: ArrayBuffer[StoragePool[_ <: SkillObject, _ <: SkillObject]],
                typesByName: HashMap[String, StoragePool[_ <: SkillObject, _ <: SkillObject]],
                dataList: ArrayBuffer[MappedInStream]): SkillFile = {

    // ensure that pools exist at all

    // trigger allocation and instance creation
    locally {
      val ts = types.iterator
      while (ts.hasNext) {
        val t = ts.next
        t.allocateData
        if (t.isInstanceOf[BasePool[_]])
          StoragePool.setNextPools(t)
      }
    }
    types.par.foreach(_.allocateInstances)

    // create restrictions (may contain references to instances)

    // read eager fields
    triggerFieldDeserialization(types, dataList)

    val r = new SkillFile(path, mode, String, Annotation, types, typesByName)
    types.par.foreach(_.ensureKnownFields(r))
    r
  }
}
