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

import java.util.Arrays
import org.apache.commons.codec.binary.Base64

import play.api._
import play.api.cache.Cache
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current

import redis.clients.jedis.JedisPubSub
import com.typesafe.plugin.RedisPlugin

object MIDaaSResponse extends Controller {

  def process = Action(parse.urlFormEncoded) { request =>
    val plugin = Play.application.plugin(classOf[RedisPlugin]).get
    val pool = plugin.sedisPool

    // check session id
    val session_id: String = (request.body("state").head)
    val sid = Cache.getAs[String](session_id).getOrElse { Done(BadRequest("invalid session")) }
   	val attrJWT:String = (request.body("attr").head)
   	val payloadb64 = attrJWT.split("\\.",3)
   	try { 
   	  if (payloadb64.length > 2) {
   	      val payload: String = new String( Base64.decodeBase64(payloadb64(1).getBytes()))    	
   	      Logger.info( "------PAYLOAD:------")
   	      Logger.info ((payload))
   	      Logger.info( "------PAYLOAD.------")
   	      // verify issuer and audience
    	  
   	      Cache.set( session_id + ".attr", payload)
   	  }
   	  
   	  Cache.set( session_id, "fulfilled")
   	  // Redis Publish
      pool.withJedisClient{ client =>
        Logger.info("firing event for mevent:" + session_id)
        client.publish("mevent", session_id)
      }
    
      Ok
   	} catch {
   	  case je: org.codehaus.jackson.JsonParseException => Logger.warn(je.toString); BadRequest("malformed JSON")
      case e:Throwable => Logger.warn(e.toString); BadRequest("invalid format")
    }
  }

}
