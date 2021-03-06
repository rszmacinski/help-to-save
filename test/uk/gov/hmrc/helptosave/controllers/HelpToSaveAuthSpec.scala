/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.helptosave.controllers

import play.api.http.Status
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AuthorisationException.fromString
import uk.gov.hmrc.helptosave.controllers.HelpToSaveAuth._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class HelpToSaveAuthSpec extends AuthSupport {

  val htsAuth = new HelpToSaveAuth(mockAuthConnector)

  private def callAuth = htsAuth.authorised { implicit request ⇒ implicit nino ⇒
    Future.successful(Ok("authSuccess"))
  }

  private def mockAuthWith(error: String) = mockAuthResultWithFail(AuthWithCL200)(fromString(error))

  "HelpToSaveAuth" should {

    "return after successful authentication" in {

      mockAuthResultWithSuccess(AuthWithCL200)(mockedNinoRetrieval)

      val result = Await.result(callAuth(FakeRequest()), 5.seconds)
      status(result) shouldBe Status.OK
    }

    "throw error if nino is not found" in {
      mockAuthResultWithSuccess(AuthWithCL200)(None)

      val result = Await.result(callAuth(FakeRequest()), 5.seconds)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    "handle various auth related exceptions and throw an error" in {

      val exceptions = List(
        "BearerTokenExpired",
        "MissingBearerToken",
        "InvalidBearerToken",
        "SessionRecordNotFound",
        "InsufficientEnrolments",
        "InsufficientConfidenceLevel",
        "UnsupportedCredentialRole",
        "unknown-blah")

      exceptions.foreach { error ⇒
        mockAuthWith(error)
        val result = callAuth(FakeRequest())
        status(result) shouldBe Status.FORBIDDEN
      }
    }
  }
}
