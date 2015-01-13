package name.alexdeleon.lib.scala.spring.mongodb.converter

import java.util
import java.util.Collections

import org.springframework.core.convert.converter.GenericConverter
import org.springframework.core.convert.{ConversionService, TypeDescriptor}

/**
 * @author Alexander De Leon <me@alexdeleon.name>
 */
class ScalaOptionConverter(conversionService: ConversionService) extends GenericConverter {

  override def getConvertibleTypes : java.util.Set[GenericConverter.ConvertiblePair] =
    new util.HashSet(util.Arrays.asList(new GenericConverter.ConvertiblePair(classOf[scala.Any], classOf[Option[_]])
      , new GenericConverter.ConvertiblePair(classOf[Option[_]], classOf[scala.Any])))

  override def convert(source: scala.Any, sourceType: TypeDescriptor, targetType: TypeDescriptor): AnyRef = if(targetType.getObjectType == classOf[Option[_]]) {
    Option(source)
  } else {
    source.asInstanceOf[Option[_]] match {
      case None => null
      case Some(obj) => conversionService.convert(obj, TypeDescriptor.forObject(obj), targetType)
    }
  }

}
