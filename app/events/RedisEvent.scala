/*
 * Copyright 2013 SecureKey Technologies Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package events


import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import play.api._
import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.{ EventSource }
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.mvc._


import libs.concurrent.Akka
import akka.actor._

import concurrent.duration.Duration
import concurrent.Future
import concurrent.Promise
import concurrent.Await

import redis.clients.jedis.JedisPubSub
import com.typesafe.plugin.RedisPlugin

object MyRedisSubscriber {

  val mobileChan = "mevent"

  val plugin = Play.application.plugin(classOf[RedisPlugin]).get
  val pool = plugin.sedisPool

  val listener = { 
    Logger.info("creating listener")
    new MyListener(Listeners.notifier)
  }
  
  val redisFuture = Future {
    Logger.info("\n ** ITS HAPPENENING ** \n")
    pool.withJedisClient{ client =>
      Logger.info("subscribing to: " + mobileChan)
      client.subscribe(listener, mobileChan)
    }
  }(Contexts.myExecutionContext)
}

object Listeners {
  lazy val notifier = Akka.system.actorOf(Props[EventActor])
}

object Contexts {
  implicit val myExecutionContext: ExecutionContext = Akka.system.dispatchers.lookup("akka.actor.redis-pubsub-context")
}

case class Listen(session: String)
case class Complete(session:String)
case class Terminate(session: String)

class EventActor extends akka.actor.Actor {
  var connected = Map.empty[String, Concurrent.Channel[String]]
  
  def receive = {
    case Listen(session) => {
      val e = Concurrent.unicast[String](
          onStart = (c) => {
            Logger.info("listener started for session:" + session)
            connected = connected + (session -> c)
          },
          onComplete = {
            Logger.info("Enum complete")
            self ! Terminate(session)
          },
          onError = (str, in) => {
            Logger.info("Error:" + str)
            self ! Terminate(session)
          }
        )
        .onDoneEnumerating( () => Logger.warn("\n **** Done Enumerating!! *** \n") )
      sender ! e
    }
    case Complete(session) => {
      Logger.info("message")
      for (c <- connected.get(session)) {
        Logger.info("pushing to listener")
        c.push("done")
      }
    }
    case Terminate(session) => {
      for (c <- connected.get(session)) {
        Logger.info("sending eof")
        c.eofAndEnd
      }
      connected = connected - session
    }
  }
}


/* Subscribe class*/
case class MyListener(notifier: akka.actor.ActorRef) extends JedisPubSub {
  
  def onMessage(channel: String, message: String): Unit = {
    Logger.info("onMessage[%s, %s]".format(channel, message))
    notifier ! Complete(message)
  }

  def onSubscribe(channel: String, subscribedChannels: Int): Unit = {
    Logger.info("onSubscribe[%s, %d]".format(channel, subscribedChannels))
  }

  def onUnsubscribe(channel: String, subscribedChannels: Int): Unit = {
    Logger.info("onUnsubscribe[%s, %d]".format(channel, subscribedChannels))
  }

  def onPSubscribe(pattern: String, subscribedChannels: Int): Unit = {
    Logger.info("onPSubscribe[%s, %d]".format(pattern, subscribedChannels))
  }

  def onPUnsubscribe(pattern: String, subscribedChannels: Int): Unit = {
    Logger.info("onPUnsubscribe[%s, %d]".format(pattern, subscribedChannels))
  }

  def onPMessage(pattern: String, channel: String, message: String): Unit = {
    Logger.info("onPMessage[%s, %s, %s]".format(pattern, channel, message))
  }
}