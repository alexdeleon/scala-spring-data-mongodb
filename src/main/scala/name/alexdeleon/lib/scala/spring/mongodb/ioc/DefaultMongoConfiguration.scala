/*
* Copyright (C) 2014 IO Informatics Inc.
*/
package name.alexdeleon.lib.scala.spring.mongodb.ioc

import DefaultMongoConfiguration._
import com.mongodb._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.core.env.Environment
import org.springframework.data.mongodb.config.AbstractMongoConfiguration
import org.springframework.data.mongodb.core.{WriteResultChecking, MongoTemplate}
import org.springframework.data.mongodb.core.convert.{DefaultMongoTypeMapper, DefaultDbRefResolver, MappingMongoConverter}

import scala.reflect.{ClassTag, classTag}
import scala.collection.JavaConversions._

/**
 * @author Alexander De Leon <me@alexdeleon.name>
 */
@Configuration
trait DefaultMongoConfiguration extends AbstractMongoConfiguration {

  val databaseName : String

  @Autowired
  var env : Environment = null

  val writeResultChecking = WriteResultChecking.EXCEPTION

  implicit def mongoConfigWrapper(config : String) = new {
    val property = s"${mongoConfigurationPrefix}.$config"

    def get[T : ClassTag](default : T) : T = {
      val value = env.getProperty(property, classTag[T].runtimeClass.asInstanceOf[Class[T]], default)
      if(value == null)
        throw new IllegalStateException(s"Missing required mongodb configuration: $property")
      value
    }

    def getOptional[T : ClassTag] : Option[T] = {
      Option(env.getProperty(property, classTag[T].runtimeClass).asInstanceOf[T])
    }

    def get : String = get[String](null)
    def getOptional = getOptional[String]
  }

  @Bean def mongoOptions : MongoClientOptions = {
    val optionsBuilder = MongoClientOptions.builder()
    optionsBuilder.connectionsPerHost(CONNECTION_PER_HOST.get[Int](20))
    optionsBuilder.connectTimeout(CONNECT_TIMEOUT.get[Int](5000))
    optionsBuilder.cursorFinalizerEnabled(CURSOR_FINALIZER_ENABLED.get[Boolean](true));

    optionsBuilder.writeConcern(writeConcern)
    optionsBuilder.maxWaitTime(MAX_WAIT_TIME.get[Int](5000))

    optionsBuilder.socketKeepAlive(SOCKET_KEEP_ALIVE.get[Boolean](true))
    optionsBuilder.socketTimeout(SOCKET_TIMEOUT.get[Int](60000))
    optionsBuilder.threadsAllowedToBlockForConnectionMultiplier(THREADS_ALLOWED_TO_BLOCK_FOR_CONNECTION_MULTIPLIER.get[Int](4))

    "mongodb.slaveOk".getOptional[Boolean] match {
      case Some(true) => optionsBuilder.readPreference(ReadPreference.secondaryPreferred())
      case _ =>
    }
    //return
    optionsBuilder.build()
  }

  def writeConcern : WriteConcern = {
    val fsync = FSYNC.get[Boolean](false)
    val j = J.get[Boolean](false)
    val w = W.get[Int](1)
    val wtimeout = W_TIMEOUT.get[Int](0)
    new WriteConcern(w, wtimeout, fsync, j)
  }


  @Bean override def mongo : MongoClient = {
    REPLICASET.getOptional match {
      case Some(value) => new MongoClient(mongoReplicaset.toList, mongoOptions)
      case _ => HOST.getOptional match {
        case Some(value) => new MongoClient(new ServerAddress(HOST.get, PORT.get[Int](27017)), mongoOptions)
        case _ => throw new IllegalArgumentException("Missing mongodb host or replicaset configuration")
      }
    }
  }

  def mongoReplicaset : Array[ServerAddress] =  {
    val replicasetConfig = REPLICASET.get
    val servers = replicasetConfig.split(",")
    for { server <- servers; hostAndPort = server.trim().split(":") } yield {
      try {
        new ServerAddress(hostAndPort(0), hostAndPort(1).toInt)
      } catch {
        case e : NumberFormatException => throw new IllegalArgumentException("Invalid port for server " + hostAndPort(0), e)
        case e : ArrayIndexOutOfBoundsException => throw new IllegalArgumentException("Missing port for server" + hostAndPort(1), e)
      }
    }
  }

  override def mappingMongoConverter : MappingMongoConverter = {
    val converter = new MappingMongoConverter(new DefaultDbRefResolver(mongoDbFactory()), mongoMappingContext())
    converter.setTypeMapper(mongoTypeMapper)
    converter.setCustomConversions(customConversions())
    converter
  }

  protected def mongoTypeMapper : DefaultMongoTypeMapper = new DefaultMongoTypeMapper(null)

  @Bean override def mongoTemplate : MongoTemplate = {
    val template = super.mongoTemplate()
    template.setWriteResultChecking(writeResultChecking)
    template
  }

  protected def mongoConfigurationPrefix = getDatabaseName

  protected override def getDatabaseName() : String = databaseName
}

object DefaultMongoConfiguration {
  final val CONNECTION_PER_HOST = "mongodb.connectionsPerHost"
  final val CONNECT_TIMEOUT = "mongodb.connectTimeout"
  final val CURSOR_FINALIZER_ENABLED = "mongodb.cursorFinalizerEnabled"
  final val MAX_WAIT_TIME = "mongodb.maxWaitTime"
  final val SOCKET_KEEP_ALIVE = "mongodb.socketKeepAlive"
  final val SOCKET_TIMEOUT = "mongodb.socketTimeout"
  final val THREADS_ALLOWED_TO_BLOCK_FOR_CONNECTION_MULTIPLIER = "mongodb.threadsAllowedToBlockForConnectionMultiplier"
  final val FSYNC = "mongodb.fsync"
  final val J = "mongodb.j"
  final val W =  "mongodb.w"
  final val W_TIMEOUT = "mongodb.wtimeout"
  final val REPLICASET = "mongodb.replicaset"
  final val HOST = "mongodb.host"
  final val PORT = "mongodb.port"
}
