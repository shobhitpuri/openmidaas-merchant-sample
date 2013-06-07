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

import lib.JoseProcessor

import redis.clients.jedis.JedisPubSub
import com.typesafe.plugin.RedisPlugin

object MIDaaSResponse extends Controller {

    val plugin = Play.application.plugin(classOf[RedisPlugin]).get
    val pool = plugin.sedisPool

  def process = Action(parse.urlFormEncoded) { request =>

    Logger.info("RESPONSE:\n" + request.body.toString)
    // check session id
    val session_id: String = (request.body("state").head)
    val sid = Cache.getAs[String](session_id).getOrElse { Done(BadRequest("invalid session")) }
   	
  
    try { 
      // parse data from JWTs.
      val vattr:JsObject = request.body.get("vattr") match {
        case Some(vjws) => Logger.info("vattr: " + vjws); JoseProcessor.getJWSData(vjws.head)
        case None => JsObject(Nil)
      }      
      Logger.info("Verfied Payload: " + vattr)
   	  
      val attr:JsObject = request.body.get("attr") match {
        case Some(v) => Logger.info("attr: " + v); JoseProcessor.getJWTData(v.head)
        case None => JsObject(Nil)
      }
      Logger.info("Unverified Payload: " + attr)

      // TBD a better way sing for composition?
      processData(session_id, 
          { (vattr \ "attrs").asOpt[JsObject] match {
              case Some(j) => j
              case None => JsObject(Nil)
            }
          } , { 
            (attr \ "attrs").asOpt[JsObject] match{
              case Some(j) => j
              case None => JsObject(Nil)
            } 
          }, 
          (vattr \ "sub").as[JsString]
      )
      
      Ok
   	} catch {
   	  case pe: java.text.ParseException => Logger.warn(pe.toString); pe.printStackTrace(); BadRequest("malformed verifed bundle")
      case e:Throwable =>  Logger.warn(e.toString); e.printStackTrace(); BadRequest("invalid format")
    }
  }

  private def processData(session_id:String, vattr:JsObject, attr:JsObject, sub:JsString) = {    
    // combine JSONs
    val allAttrs = (attr ++ vattr) + ("subject", sub) + {
      (vattr \ "email").asOpt[String] match {
        case Some(e) => ("isEmailVerified", JsBoolean(true))
        case None => ("isEmailVerified", JsBoolean(false))
      }
    }
        
    Logger.info("processed data: " + allAttrs.toString)    
    Cache.set( session_id, "fulfilled")
    Cache.set( session_id + ".data", allAttrs.toString)
    // Redis Publish
    pool.withJedisClient{ client =>
        Logger.info("firing event for mevent:" + session_id)
        
        client.publish("mevent", session_id)
    }
  }
  
  
}
