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
  

  def index = Action { request =>
    val s:String = request.session.get("session").getOrElse {
    	val uuid = UUID.randomUUID().toString()
    	Cache.set(uuid, "", 600)  
    	Logger.info("New Session: %s".format(uuid))
    	uuid
    } 
        
    Ok(Scalate("index.jade").render()).withSession(request.session + ("session" -> s))
  }

  // creates a new checkout session
  def checkout = Action { request =>
    val BASE_QR_URL = "https://qrcode.kaywa.com/img.php?s=8&t=p&d=https%3A%2F%2Fmidaas-merchant.securekeylabs.com%2Fr%2F"
    
    val session_id: String = request.session.get("session").getOrElse { throw new Exception("missing session")}  
    
    val code: Option[String] = Cache.getAs[String](session_id) 
    if (code.isEmpty)
    {
    	def getCode: String = {
  	  		val c = Random.alphanumeric.take(5).mkString
  	  		if (Cache.get(c) == None) c
  	  		else getCode
    	}

    	val c = getCode

    	// map the short code to the UUID
    	Cache.set(session_id, c, 600)
    	Cache.set(c, session_id, 600)
    }
    
    // store the items and prices in the Cache
  	
    // return the image to render.
  	val qrUrl = BASE_QR_URL + code
    println("Request:" + request.body)
    Ok(Json.obj( "url" ->  qrUrl)).withSession(request.session)
  }

  def listen = Action { request =>
  	val plugin = Play.application.plugin(classOf[RedisPlugin]).get
    val pool = plugin.sedisPool

    val session_id = request.session.get("session").getOrElse { 
  		println("missing session")
  	    BadRequest("missing session") 
  	  }
    
    val redisPromise:Promise[Option[String]] = Promise()
    val listener = new MyListener(redisPromise)
    Future { 
      pool.withJedisClient{ client =>
      	client.subscribe(listener, session_id.toString)
  	    println("\nsubscribe returned\n")
  	  }
    }(Contexts.myExecutionContext)

  	val myAlert = 
 	  Enumerator.generateM {
  		if (redisPromise.isCompleted) Future.successful(None)
  		else redisPromise.future
  	}
  	    
    Ok.feed(Enumerator(":" + new String().padTo(2049, ' ') + "\nretry: 2000\n") andThen myAlert &> EventSource())
      .as("text/event-stream")
      .withHeaders("Cache-Control" -> "no-cache", "Access-Control-Allow-Origin" -> "*")
   }

  def fulfill = Action { request =>
    Ok("You are fulfilled")
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


