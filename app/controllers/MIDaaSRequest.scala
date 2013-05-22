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
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc._
import play.api.Play.current


object MIDaaSRequest extends Controller {
  val CLIENT_ID = "openmidaas-merchant-sample.securekeylabs.com"
  
  // return a MIDaaS request object for the session 
  def process(id: String) = Action { request =>
    val session_id = Cache.getAs[String](id)
    
    if (None == session_id) {
      BadRequest("unknown id")
    }
    else if ("fulfilled" == session_id.get) {
      BadRequest("already fulfilled") 
    }
    else
    {
    	val req:JsValue = Json.obj(
    			"client_id" -> CLIENT_ID,
    			"acr" -> 1,
    			"attrs" -> Json.obj(
    					"email" -> Json.obj("essential" -> true, "verified" -> true),
    					"shipping_address" -> Json.obj("type" -> "address", "label" -> "Shipping Address"),
    					"credit_card" -> Json.obj("label" -> "Credit Card"),
    					"billing_address" -> Json.obj("label" -> "Billing Address")
    			),
    			"state" -> session_id,
    			"return" -> Json.obj(
    					"method" -> "postback",
    					"url" -> ("https://midaas-merchant.securekeylabs.com/postback") // postback url from config
    			)
    	)
         
    	Ok(req)
    }
  }

}
