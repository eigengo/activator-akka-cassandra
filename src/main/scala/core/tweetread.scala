package core

import akka.actor.Actor
import com.datastax.driver.core.{BoundStatement, Cluster, Row}
import domain.Tweet
import core.TweetReadActor.{CountAll, FindAll}
import com.datastax.driver.core.querybuilder.QueryBuilder

object TweetReadActor {
  case class FindAll(maximum: Int = 100)
  case object CountAll
}

class TweetReadActor(cluster: Cluster) extends Actor {
  val session = cluster.connect(Keyspaces.akkaCassandra)
  val countAll  = new BoundStatement(session.prepare("select count(*) from tweets;"))

  import scala.collection.JavaConversions._
  import cassandra.resultset._
  import context.dispatcher

  def buildTweet(r: Row): Tweet = {
    val id = r.getString("key")
    val user = r.getString("user_user")
    val text = r.getString("text")
    val createdAt = r.getDate("createdat")
    Tweet(id, user, text, createdAt)
  }

  def receive: Receive = {
    case FindAll(maximum)  =>
      val realSender = sender
      val query = QueryBuilder.select().all().from(Keyspaces.akkaCassandra, "tweets").limit(maximum)
      session.executeAsync(query) onSuccess {
        case rs => realSender ! Right(rs.all().map(buildTweet).toList)
      }
      // sender ! session.execute(selectAll).all().map(buildTweet).toList
    case CountAll =>
      val realSender = sender
      session.executeAsync(countAll) onSuccess {
        case rs => realSender ! rs.one.getLong(0)
      }
      // sender ! session.execute(countAll).one.getLong(0)
  }
}
