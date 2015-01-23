package name.alexdeleon.lib.scala.spring.mongodb.converter

import java.util

import org.springframework.core.convert.converter.Converter
import scala.collection.JavaConverters._

/**
 * @author Alexander De Leon <me@alexdeleon.name>
 */
class JavaToScalaCollectionConverter extends Converter[util.ArrayList[_], Seq[_]]{
  override def convert(source: util.ArrayList[_]): Seq[_] = source.asScala
}
