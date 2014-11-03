package core

import com.datastax.driver.core.{ProtocolOptions, Session, Cluster}
import org.specs2.specification.{SpecificationStructure, Fragments, Step}
import scala.io.Source
import java.io.File
import akka.actor.ActorSystem

trait TestCassandraCluster extends CassandraCluster {
  def system: ActorSystem

  private def config = system.settings.config

  import scala.collection.JavaConversions._
  private val cassandraConfig = config.getConfig("akka-cassandra.test.db.cassandra")
  private val port = cassandraConfig.getInt("port")
  private val hosts = cassandraConfig.getStringList("hosts").toList

  lazy val cluster: Cluster =
    Cluster.builder().
      addContactPoints(hosts: _*).
      withCompression(ProtocolOptions.Compression.SNAPPY).
      withPort(port).
      build()

}

trait CleanCassandra extends SpecificationStructure {
  this: CassandraCluster =>

  private def runCql(session: Session, file: File): Unit = {
    val query = Source.fromFile(file).mkString
    query.split(";").foreach(session.execute)
  }

  private def runAllCqls(): Unit = {
    val session = cluster.connect(Keyspaces.akkaCassandra)
    val uri = getClass.getResource("/").toURI
    new File(uri).listFiles().foreach { file =>
      if (file.getName.endsWith(".cql")) runCql(session, file)
    }
    session.close()
  }

  override def map(fs: => Fragments) = super.map(fs) insert Step(runAllCqls())
}
