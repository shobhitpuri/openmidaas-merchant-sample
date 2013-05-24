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

package controllers

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
import akka.pattern.ask
import akka.util.Timeout

import concurrent.duration.Duration
import concurrent.Future
import concurrent.Promise
import concurrent.Await

object LiveListen extends Controller {

  import events._
  val rs = MyRedisSubscriber

  implicit val timeout = Timeout(1 minute)  
  // var eventSources:Map[String, Enumeratee[String, String]] = Map()
  // Enumerator(":" + new String().padTo(2049, ' ') + "\nretry: 2000\n") andThen 
  def startEventSource(alert: Enumerator[String]) =
     Ok.feed(alert &> EventSource())
       .as("text/event-stream")
       .withHeaders(
          "Cache-Control" -> "no-cache",
          "Connection"    -> "keep-alive",
          "Access-Control-Allow-Origin" -> "*"
       )
  
  /**
   * Client will attach an EventSource to listen for out-of-band events
   */
  def listen = Action { request =>
      val session_id: String = request.session.get("session").getOrElse { throw new Exception("missing session") }
      val sessionState: String = Cache.getAs[String](session_id).getOrElse { throw new Exception("session expired") }
    
      val timeoutFuture = play.api.libs.concurrent.Promise.timeout("expired", 5.minutes)
      timeoutFuture.onComplete {
        case t => Listeners.notifier ! Terminate(session_id)
      }
    
      AsyncResult {
        (Listeners.notifier ? Listen(session_id)).mapTo[Enumerator[String]].map(startEventSource)
      }
  }
}
