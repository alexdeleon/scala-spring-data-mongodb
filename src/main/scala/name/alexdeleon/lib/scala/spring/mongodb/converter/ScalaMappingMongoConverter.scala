package name.alexdeleon.lib.scala.spring.mongodb.converter


import java.util

import com.mongodb.DBObject
import org.springframework.data.mapping.context.MappingContext
import org.springframework.data.mongodb.core.convert.{DbRefResolver, MappingMongoConverter}
import org.springframework.data.mongodb.core.mapping.{MongoPersistentProperty, MongoPersistentEntity}
import scala.collection.JavaConverters._

/**
 * @author Alexander De Leon <me@alexdeleon.name>
 */
class ScalaMappingMongoConverter(dbRefResolver: DbRefResolver,
                                 mappingContext: MappingContext[_ <: MongoPersistentEntity[_], MongoPersistentProperty])
  extends MappingMongoConverter(dbRefResolver, mappingContext){

  override def write(obj: Object, dbo: DBObject): Unit = asJavaAndDelegate(obj, (o) => {
    super.write(o, dbo)
  })

  override def writePropertyInternal(obj: Object, dbo: DBObject, prop: MongoPersistentProperty): Unit = asJavaAndDelegate(obj, (o) => {
    super.writePropertyInternal(o, dbo, prop)
  })

  override def read[S](clazz: Class[S], dbo: DBObject): S = delegateAndConvertToScala[S](clazz, (c: Class[_]) => super.read(c, dbo))

  private def asJavaAndDelegate[T](obj: scala.Any, delegate: (scala.Any) => T ): T = delegate(obj match {
    case col: Iterable[_] => col.asJava
    case o => o
  })

  private def delegateAndConvertToScala[T](clazz: Class[T], delegate: (Class[_]) => _): T = clazz match {
    case c if classOf[Seq[_]].isAssignableFrom(c) => delegate(classOf[util.List[_]]).asInstanceOf[util.List[_]].asScala.toSeq.asInstanceOf[T]
    case c => delegate(c).asInstanceOf[T]
  }



}
