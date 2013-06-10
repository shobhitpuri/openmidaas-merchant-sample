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

package model

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Address(
    street:    String,
    region:    String,
    locality:  String,
    country:   String,
    code:      String
)
object Address {
  implicit val addressReads = (
    ( __ \ "streetAddress").read[String] and
    ( __ \ "region").read[String] and
    ( __ \ "locality").read[String] and
    ( __ \ "country").read[String] and
    ( __ \ "postalCode" ).read[String]
  )(Address.apply _)
  
  implicit val addressWrites = new Writes[Address] {
    def writes(addr: Address) : JsValue = {
      Json.obj(
        "streetAddress" -> addr.street,
        "region" -> addr.region,
        "locality" -> addr.locality,
        "country" -> addr.country,
        "postalCode" -> addr.code
      )
    }  
  }
}

case class CreditCard(
    typ:             Option[String],
    cardNo:          String,
    expYear:         String,
    expMonth:        String,
    cardHolderName:  String, 
    secure:          Option[String]
)
object CreditCard {
  implicit val creditCardReads = (
      ( __ \ "type" ).readNullable[String] and
      ( __ \ "creditCard" ).read[String] and
      ( __ \ "expiryYear" ).read[String] and
      ( __ \ "expiryMonth").read[String] and
      ( __ \ "cardHolderName").read[String] and
      ( __ \ "cardCvv").readNullable[String]
  )(CreditCard.apply _)
  
  implicit val creditCardWrites = new Writes[CreditCard] {
    def writes(cc: CreditCard) : JsValue = {
      Json.obj(
        "type" -> cc.typ,
        "creditCard" -> cc.cardNo,
        "expYear" -> cc.expYear,
        "expMonth" -> cc.expMonth,
        "cardHolderName" -> cc.cardHolderName,
        "cardCvv" -> cc.secure
      )
    }
  }
}

case class Customer(
    email:          Option[String],
    isEmailVerified: Boolean,
    phone:          Option[String],
    creditCard:     Option[CreditCard],
    shippingAddr:   Option[Address],
    billAddr:       Option[Address]
)
object Customer {
    implicit val customerReads = (
      ( __ \ "email").readNullable[String] and
      ( __ \ "isEmailVerified").read[Boolean] and
      ( __ \ "phone_number").readNullable[String] and
      ( __ \ "credit_card").readNullable[CreditCard] and
      ( __ \ "shipping_address" ).readNullable[Address] and
      ( __ \ "billing_address").readNullable[Address]
    )(Customer.apply _)
    
    implicit val customerWrites = new Writes[Customer] {
      def writes(cust: Customer) : JsValue = {
        Json.obj(
          "email" -> cust.email,
          "isEmailVerfied" -> cust.isEmailVerified,
          "phone_number" -> cust.phone,
          "credit_card" -> cust.creditCard,
          "shipping_address" -> cust.shippingAddr,
          "billing_address" -> cust.billAddr
        )
      }
    }
}