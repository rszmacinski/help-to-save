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

import play.api.Configuration
import play.api.mvc.Results._
import play.api.mvc.{AnyContentAsEmpty, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.Retrievals.allEnrolments
import uk.gov.hmrc.helptosave.controllers.StrideAuthSpec.NotLoggedInException
import uk.gov.hmrc.helptosave.utils.TestSupport
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class StrideAuthSpec extends TestSupport {

  val roles = List("a", "b")

  override lazy val additionalConfig: Configuration = Configuration("stride.roles" → roles)

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  def mockAuthorised[A](expectedPredicate: Predicate,
                        expectedRetrieval: Retrieval[A])(result: Either[Throwable, A]) =
    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[A])(_: HeaderCarrier, _: ExecutionContext))
      .expects(expectedPredicate, expectedRetrieval, *, *)
      .returning(result.fold(Future.failed, Future.successful))

  class TestStrideAuth(roles: List[String]) extends StrideAuth(mockAuthConnector) {

    override val authConnector: AuthConnector = mockAuthConnector

  }

  "StrideAuth" must {

    val call = Call("GET", "url")

    lazy val test = new TestStrideAuth(roles)

    lazy val action = test.authorisedFromStride { _ ⇒ Ok }

    "provide a authorised method" which {

      "forbids user from accessing the resource if no active session found" in {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
        mockAuthorised(AuthProviders(PrivilegedApplication), allEnrolments)(Left(NotLoggedInException))

        val result = action(request)
        status(result) shouldBe FORBIDDEN
      }

      "returns an Unauthorised status" when {

        "the requester does not have the necessary roles" in {
          List(
            Set("c"),
            Set("d"),
            Set("e"),
            Set.empty
          ).foreach { enrolments ⇒
              mockAuthorised(AuthProviders(PrivilegedApplication), allEnrolments)(Right(Enrolments(enrolments.map(Enrolment(_)))))

              status(action(FakeRequest())) shouldBe UNAUTHORIZED
            }
        }

      }

      "allow authorised logic to be run if the requester has the correct roles" in {
        mockAuthorised(AuthProviders(PrivilegedApplication), allEnrolments)(Right(Enrolments(roles.map(Enrolment(_)).toSet)))

        status(action(FakeRequest())) shouldBe OK
      }

    }

  }

}

object StrideAuthSpec {

  case object NotLoggedInException extends NoActiveSession("uh oh")

}

