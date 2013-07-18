package core

import akka.actor.Actor
import com.datastax.driver.core.{BoundStatement, Cluster, Row, Session}
import domain.Tweet
import core.TweetReadActor.{CountAll, FindAll}
import scala.collection.JavaConversions

object TweetReadActor {
  case object FindAll
  case object CountAll
}

class TweetReadActor(cluster: Cluster) extends Actor {
  val session = cluster.connect(Keyspaces.akkaCassandra)
  val selectAll = new BoundStatement(session.prepare("select * from tweets;"))
  val countAll  = new BoundStatement(session.prepare("select count(*) from tweets;"))
  import JavaConversions._

  def buildTweet(r: Row): Tweet = {
    val id = r.getString("key")
    val user = r.getString("user_user")
    val text = r.getString("text")
    val createdAt = r.getDate("createdat")
    Tweet(id, user, text, createdAt)
  }

  def receive: Receive = {
    case FindAll  =>
      sender ! Right(session.execute(selectAll).all().map(buildTweet).toList)
    case CountAll =>
      sender ! session.execute(countAll).one.getLong(0)
  }
}
