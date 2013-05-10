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

import play.api._
import play.api.cache.Cache
import play.api.Play.current
import play.api.libs.json._
import play.api.mvc._

import redis.clients.jedis.JedisPubSub
import com.typesafe.plugin.RedisPlugin

object MIDaaSResponse extends Controller {

  def process = Action(parse.urlFormEncoded) { request =>
    println(request.body)
    val plugin = Play.application.plugin(classOf[RedisPlugin]).get
    val pool = plugin.sedisPool
    
    // check session id
    val session_id: Option[String] = Option[String](request.body("state").head)
    if (None == session_id)
    {
      BadRequest("missing session")
    }
    else
    {
    	val sid = Cache.getAs[String](session_id.get) getOrElse { BadRequest("invalid session") } 
    	Cache.set( session_id + ".attr", Option[String](request.body("attr").head) getOrElse "")
    	// Redis Publish
    	pool.withJedisClient{ client =>
    		client.publish(session_id.get, "done")
    	}
    
    	Ok
    }
  }

}
