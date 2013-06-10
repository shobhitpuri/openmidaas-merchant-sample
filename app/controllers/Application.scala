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

import lib.Scalate
import model._

object Application extends Controller {
    
  def index = Action { implicit request =>
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
  def checkout = Action { implicit request =>
    val BASE_URL = "https://midaas-merchant.securekeylabs.com/r/"
    val BASE_QR_URL = "https://qrcode.kaywa.com/img.php?s=8&t=p&d="
    
    val session_id: String = request.session.get("session").getOrElse { throw new Exception("missing session")}  
    
    Cache.getAs[String](session_id).map { sessionState => 
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
    }.getOrElse { 
      Logger.warn("expired session, refresh"); 
      Redirect("/") 
    }
  }


  /**
   * render the fulfillment page
   */
  def fulfill = Action { implicit request =>
    
    val session_id: String = request.session.get("session").getOrElse { throw new Exception("missing session")}  
    Cache.getAs[String](session_id).map { sessionState => 
      Cache.getAs[String](session_id + ".data").map { sessionData =>
          Logger.info(sessionData)
          val data = Json.parse(sessionData)
          val customer = Json.fromJson[Customer](data).map { c => c }.recoverTotal{ e => Logger.warn(e.toString); throw new Exception("bad data");}
          
          Ok(Scalate("fulfill.jade").render('customer -> customer))
      }.getOrElse { 
          BadRequest("Payment Data Not Available") 
      }
    }.getOrElse { 
      Logger.warn("expired session, refresh"); 
      BadRequest("Session Expired")
    }
  }

}



