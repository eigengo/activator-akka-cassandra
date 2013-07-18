package core

import akka.actor.{Actor, Props, ActorSystem}
import spray.can.Http
import spray.http._
import spray.http.HttpRequest
import spray.http.HttpResponse
import akka.routing.RoundRobinRouter
import akka.io.IO
import scala.io.Source

class TwitterApi private(system: ActorSystem, port: Int, body: String) {

  val blackHoleActor = system.actorOf(Props(new Actor {
    def receive: Receive = Actor.emptyBehavior
  }))

  private class Service extends Actor {

    def receive: Receive = {
      case _: Http.Connected =>
        sender ! Http.Register(self)
      case HttpRequest(HttpMethods.GET, _, _, _, _) =>

        sender ! HttpResponse(entity = HttpEntity(body))
      case _ =>
    }
  }

  private val service = system.actorOf(Props(new Service).withRouter(RoundRobinRouter(nrOfInstances = 50)))
  private val io = IO(Http)(system)
  io.tell(Http.Bind(service, "localhost", port = port), blackHoleActor)

  def stop(): Unit = {
    io.tell(Http.Unbind, blackHoleActor)
    system.stop(service)
    system.stop(io)
  }
}

object TwitterApi {

  def apply(port: Int)(implicit system: ActorSystem): TwitterApi = {
    val body = Source.fromInputStream(getClass.getResourceAsStream("/tweets.json")).mkString
    new TwitterApi(system, port, body)
  }

}
