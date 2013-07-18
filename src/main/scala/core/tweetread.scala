package core

import akka.actor.Actor
import com.datastax.driver.core.{Cluster, Row, Session}
import domain.Tweet
import core.TweetReadActor.{CountAll, FindAll}

object TweetReadActor {
  case object FindAll
  case object CountAll
}

class TweetReadActor(cluster: Cluster) extends Actor with TweetReadOperations {
  val session = cluster.connect(Keyspaces.akkaCassandra)

  def receive: Receive = {
    case FindAll => sender ! findAllTweets
    case CountAll => sender ! countAllTweets
  }
}

private[core] trait TweetReadOperations extends CassandraCrud {
  import com.datastax.driver.core.querybuilder.{ QueryBuilder => QB }

  implicit def session: Session

  def buildTweet(r: Row): Tweet = {
    val id = r.getString("key")
    val user = r.getString("user_user")
    val text = r.getString("text")
    val createdAt = r.getDate("createdat")
    Tweet(id, user, text, createdAt)
  }

  def findAllTweets(): Either[ErrorMessage, List[Tweet]] = {
    val query = QB.select.all.from(ColumnFamilies.tweets)
    (gather(buildTweet) &= enumRS(session.execute(query))).runEither
  }

  def countAllTweets(): Long = {
    val query = QB.select.countAll().from(ColumnFamilies.tweets)
    session.execute(query).one.getLong(0)
  }

}