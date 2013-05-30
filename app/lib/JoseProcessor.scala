package lib

import java.net.URL
import java.security.interfaces._

import scala.collection.JavaConversions._

import play.api.cache.Cache
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws._
import play.api.Play.current
import play.Logger

import concurrent.Await
import concurrent.Future
import concurrent.Promise
import concurrent.duration._

import com.nimbusds.jose.crypto._
import com.nimbusds.jose.jwk._
import com.nimbusds.jose._
import com.nimbusds.jwt._



// Exception Classes

class MalformedException extends Exception
class SigFailedException extends Exception
class NotFoundException extends Exception


object JoseProcessor {
  
  val KEY_TIMEOUT = (60)
  
  /**
   * @param the base64url encoded JWS
   * @return the JSON payload from a JWS signed with a key specified by a 'jku' and 'kid'.
   * @throws NotFoundException if the key cannot be retrieved.
   * @throws MalformedException if it cannot parse the JWS or the header is missing the jku/kid.
   * @throws SigFailedException if the signature fails.
   */
  def getJWSData(jws: String): JsValue = {
    try {
      // create JWS object from string
      val jwsObject = JWSObject.parse(jws)

      val header = jwsObject.getHeader
      
      // get header data
      val keyurl = header.getJWKURL
      val keyid = header.getKeyID
      
      // get the key
      val key:RSAPublicKey = getKey(keyurl, keyid)
      
      // create verifier
      val rsaVerifier = new RSASSAVerifier(key)
      
      // verify JWS
      jwsObject.verify(rsaVerifier)
      
      // create JSON data from payload
      Json.parse(jwsObject.getPayload.toString)
      
    } catch {
      case e: Throwable => throw e
    }

  }
  
  /**
   * @param jwt the base64url jwt value
   * @return JSON object of the payload
   * @throws MalformedException if the JWT cannot be parsed
   */
  def getJWTData(jwt:String): JsValue = {
    val jwtObject:PlainJWT = PlainJWT.parse(jwt)
    Json.parse(jwtObject.getPayload().toString) 
  }
  
  
  def getKey(keyurl:URL, kid:String):RSAPublicKey = {
    
    // check cache for key
    def getJWKString:String = {
      Cache.getAs[String]("jwskeys." + keyurl + "." + kid ) match {
        case Some(jwk:String) => {
          Logger.info("key found in cache"); 
          jwk
        }
        case None => {
           Await.result( WS.url(keyurl.toString).get().map[String] { response => 
             if (200 != response.status) {
               Logger.warn(s"failed to get JWK @ {$keyurl} code (${response.status}" )
               throw new NotFoundException()
             }
             
             val jwkset:List[JWK] = (JWKSet.parse(response.body)).getKeys().toList
             val jwk = jwkset.head.toString
             Cache.set("jwskeys." + keyurl + "."+ kid, jwk, KEY_TIMEOUT)
             jwk
           }, 1.minute)
        }       
      }
    }

    com.nimbusds.jose.jwk.RSAKey.parse(getJWKString).toRSAPublicKey()
  }
}