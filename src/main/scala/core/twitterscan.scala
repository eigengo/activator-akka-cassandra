package core

import spray.httpx.unmarshalling.{MalformedContent, Unmarshaller, Deserialized}
import spray.http.HttpEntity
import spray.json._
import domain.Tweet
import scala.Some
import java.text.SimpleDateFormat

trait TweetMarshaller {
  type Tweets = List[Tweet]

  implicit object TweetUnmarshaller extends Unmarshaller[Tweets] {

    val dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss Z yyyy")

    def mkTweet(status: JsValue): Deserialized[Tweet] = {
      val json = status.asJsObject
      (json.fields.get("id_str"), json.fields.get("text"), json.fields.get("created_at"), json.fields.get("user")) match {
        case (Some(JsString(id)), Some(JsString(text)), Some(JsString(createdAt)), Some(JsObject(user))) =>
          user.get("id_str") match {
            case Some(JsString(userId)) => Right(Tweet(id, userId, text, dateFormat.parse(createdAt)))
            case _                      => Left(MalformedContent("Bad tweet JSON"))
          }
        case _                          => Left(MalformedContent("Bad status JSON"))
      }
    }

    def apply(entity: HttpEntity): Deserialized[Tweets] = {
      val json = JsonParser(entity.asString).asJsObject
      json.fields.get("statuses") match {
        case Some(JsArray(statuses)) => Right(statuses.map(t => mkTweet(t).right.get))
        case _                       => Left(MalformedContent("statuses missing"))
      }
    }
  }

}

class TwitterScan {
  // http://twitter-search-proxy.herokuapp.com/search/tweets?q=typesafe
  //private val pipeline = sendReceive ~> unmarshal[Tweets]
}
