/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.helptosaveeligibilitycheck.models

import play.api.libs.json.{Format, Json}

case class Email(email: String) extends AnyVal

object Email {
  implicit val emailFormat: Format[Email] = Json.format[Email]
}

case class EmailException(id: String, errors: String) extends Exception(s"Could not find email in the user details for id: $id, errors: $errors ")