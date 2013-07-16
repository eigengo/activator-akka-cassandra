package core

import com.datastax.driver.core.Cluster
import akka.testkit.TestKit

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
