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

import java.net.URLEncoder
import java.util.UUID

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.Random

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

import concurrent.duration.Duration
import concurrent.Future
import concurrent.Promise
import concurrent.Await

import redis.clients.jedis.JedisPubSub
import com.typesafe.plugin.RedisPlugin


object Application extends Controller {
  

  var eventSources:Map[String, Enumeratee[String, String]] = Map()
  
  def index = Action { request =>
    val sessionFromCookie = request.session.get("session")
    def getSession(): String = {
      if (sessionFromCookie == None || Cache.getAs[String](sessionFromCookie.get) == None) {
    	val uuid = UUID.randomUUID().toString()
    	Cache.set(uuid, "shopping", 600)  
    	Logger.info("New Session: %s".format(uuid))
    	uuid
      } else {
        sessionFromCookie.get
      }
    }
    
    Ok(Scalate("index.jade").render()).withSession(request.session + ("session" -> getSession))
  }

  /**
   *  creates a new checkout session by generating a short code and 
   *  providing a URL to the QR code.
   */ 
  def checkout = Action { request =>
    val BASE_URL = "https://midaas-merchant.securekeylabs.com/r/"
    val BASE_QR_URL = "https://qrcode.kaywa.com/img.php?s=8&t=p&d="
    
    val session_id: String = request.session.get("session").getOrElse { throw new Exception("missing session")}  
    
    val sessionState = Cache.getAs[String](session_id).getOrElse{ Logger.warn("expired session, refresh"); Done(Redirect("/")) }
    val code: String = Cache.getAs[String](session_id + ".shortcode").getOrElse {
    	def getCode: String = {
  	  		val c = Random.alphanumeric.take(5).mkString
  	  		if (Cache.get(c) == None) c
  	  		else getCode
    	}

    	val c = getCode

    	// map the short code to the UUID
    	Cache.set(session_id + ".shortcode", c, 600)
    	Cache.set(c, session_id, 600)
    	Logger.info(" generated short code[" + c + "] for session " + session_id)
    	c
    }
    
    // store the items and prices in the Cache
  	assert(! code.isEmpty )
    // return the image to render.
  	val url = BASE_URL + code
    val qrUrl = BASE_QR_URL + URLEncoder.encode(url, "UTF-8") 
    Ok(Json.obj( "url" ->  url, "img_url" -> qrUrl)).withSession(request.session)
  }

  
  /**
   * Client will attach an EventSource to listen for out-of-band events
   */
  def listen = Action { request =>
  	val plugin = Play.application.plugin(classOf[RedisPlugin]).get
    val pool = plugin.sedisPool

    val session_id: String = request.session.get("session").getOrElse { throw new Exception("missing session") }
    val sessionState: String = Cache.getAs[String](session_id).getOrElse { throw new Exception("session expired") }
  	
  	val eventSource = eventSources.get(session_id) match {
  	  case Some(es) => es
  	  case None => {
  	    Logger.info("Creating Event Source for session: " + session_id)
  	    val newES = EventSource[String]()
  	    this.eventSources = this.eventSources + (session_id -> newES) 
  	    newES
  	  }
  	}
    
    val redisPromise:Promise[Option[String]] = Promise()
    val listener = new MyListener(redisPromise)
    val redisFuture = Future { 
      pool.withJedisClient{ client =>
        Logger.info("subscribing to: " + session_id)
        client.subscribe(listener, session_id)
  	  }
    }(Contexts.myExecutionContext)
    val timeoutFuture = play.api.libs.concurrent.Promise.timeout("expired", 5.minutes)
    timeoutFuture.onComplete {
      case t => redisPromise.success(Some("expired"))
    }
    
  	val myAlert = 
 	  Enumerator.generateM {
  		if (redisPromise.isCompleted) {
  		  // terminate the enumeratee and remove from the map
  		  Logger.info("Removing Event Source for session:" + session_id)
  		  this.eventSources = this.eventSources - (session_id)
  		  Future.successful(None)
  		}
  		else {
  		  redisPromise.future
  		}
  	}
  	
  	
  	myAlert.onDoneEnumerating( () => Logger.warn("\n **** Done Enumerating!! *** \n") )
  	
  	Ok.feed(Enumerator(":" + new String().padTo(2049, ' ') + "\nretry: 2000\n") andThen myAlert &> eventSource)
      .as("text/event-stream")
      .withHeaders("Cache-Control" -> "no-cache", "Access-Control-Allow-Origin" -> "*")
   }

  def fulfill = Action { request =>
    Ok(Scalate("fulfill.jade").render())
  }

}

// Execution context used to avoid blocking on subscribe
object Contexts {
  implicit val myExecutionContext: ExecutionContext = Akka.system.dispatchers.lookup("akka.actor.redis-pubsub-context")
}

/* Subscribe class*/
case class MyListener(p: Promise[Option[String]]) extends JedisPubSub {
  val promise: Promise[Option[String]] = p
  
  def onMessage(channel: String, message: String): Unit = {
    Logger.info("onMessage[%s, %s]".format(channel, message))
    promise.success(Some(message))
    unsubscribe
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


