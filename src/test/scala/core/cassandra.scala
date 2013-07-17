package core

import com.datastax.driver.core.{Query, Session, Cluster}
import akka.testkit.TestKit
import org.specs2.specification.{SpecificationStructure, Fragments, Step}
import scala.io.Source
import java.io.File

trait TestCassandraCluster extends CassandraCluster {
  this: TestKit =>

  private def config = system.settings.config

  import scala.collection.JavaConversions._
  private val cassandraConfig = config.getConfig("akka-cassandra.test.db.cassandra")
  private val port = cassandraConfig.getInt("port")
  private val hosts = cassandraConfig.getStringList("hosts").toList

  lazy val cluster: Cluster =
    Cluster.builder().
      addContactPoints(hosts: _*).
      withPort(port).
      build()

}

trait CleanCassandra extends SpecificationStructure {
  this: CassandraCluster =>

  private def runClq(session: Session, file: File): Unit = {
    val query = Source.fromFile(file).mkString
    query.split(";").foreach(session.execute)
  }

  private def runAllClqs(): Unit = {
    val session = cluster.connect(Keyspaces.akkaCassandra)
    val uri = getClass.getResource("/").toURI
    new File(uri).listFiles().foreach { file =>
      if (file.getName.endsWith(".cql")) runClq(session, file)
    }
    session.shutdown()
  }

  override def map(fs: => Fragments) = super.map(fs) insert Step(runAllClqs())
}
