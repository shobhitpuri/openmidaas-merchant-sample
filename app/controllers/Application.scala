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

import play.api._
import play.api.Play.current
import play.api.cache.Cache
import play.api.mvc._

import scala.util.Random

object Application extends Controller {
  
  def index = Action {
    Cache.set("mykey", "My value")
    val s = Cache.getAs[String]("mykey")
    Logger.info("Value retrieved from Cache: %s".format(s))
    
    Ok(Scalate("index.jade").render())
  }

  // creates a new checkout session
  def checkout = Action { request =>
  	val uuid = UUID.randomUUID().toString()
    
    def getCode: String = {
  	  val c = Random.alphanumeric.take(5).mkString
      if (Cache.getAs[String](c) == None) c
      else getCode
    }

    val code = getCode

  	// map the short code to the UUID
  	Cache.set(uuid, code, 600)
  	Cache.set(code, uuid, 600)

  	// store the items and prices in the Cache
  	
  	// return the image to render.
  	
    println("Request:" + request.body)
  	Ok
  }

  def fulfill = Action { request =>
    Ok
  }
  
}

