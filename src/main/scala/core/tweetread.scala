package core

import akka.actor.Actor
import com.datastax.driver.core.{ResultSet, BoundStatement, Cluster, Row}
import domain.Tweet
import core.TweetReaderActor.{CountAll, FindAll}
import com.datastax.driver.core.querybuilder.QueryBuilder

object TweetReaderActor {
  case class FindAll(maximum: Int = 100)
  case object CountAll
}

class TweetReaderActor(cluster: Cluster) extends Actor {
  val session = cluster.connect(Keyspaces.akkaCassandra)
  val countAll  = new BoundStatement(session.prepare("select count(*) from tweets;"))

  import scala.collection.JavaConversions._
  import cassandra.resultset._
  import context.dispatcher
  import akka.pattern.pipe

  def buildTweet(r: Row): Tweet = {
    val id = r.getString("key")
    val user = r.getString("user_user")
    val text = r.getString("text")
    val createdAt = r.getDate("createdat")
    Tweet(id, user, text, createdAt)
  }

  def receive: Receive = {
    case FindAll(maximum)  =>
      val query = QueryBuilder.select().all().from(Keyspaces.akkaCassandra, "tweets").limit(maximum)
      session.executeAsync(query) map(_.all().map(buildTweet).toList) pipeTo sender
    case CountAll =>
      session.executeAsync(countAll) map(_.one.getLong(0)) pipeTo sender
  }
}
