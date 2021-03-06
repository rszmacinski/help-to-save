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

package uk.gov.hmrc.helptosave.connectors

import cats.instances.int._
import cats.syntax.eq._
import org.joda.time.LocalDate
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.helptosave.models.{EligibilityCheckResult, UCResponse}
import uk.gov.hmrc.helptosave.utils.{MockPagerDuty, TestSupport}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class EligibilityCheckConnectorSpec extends TestSupport with GeneratorDrivenPropertyChecks with ServicesConfig with MockPagerDuty {
  MdcLoggingExecutionContext
  val date = new LocalDate(2017, 6, 12) // scalastyle:ignore magic.number

  lazy val connector = new EligibilityCheckConnectorImpl(mockHttp, mockMetrics, mockPagerDuty)

  def mockGet(url: String)(response: Option[HttpResponse]) =
    (mockHttp.get(_: String, _: Map[String, String])(_: HeaderCarrier, _: ExecutionContext))
      .expects(url, connector.desHeaders, *, *)
      .returning(response.fold(Future.failed[HttpResponse](new Exception("")))(Future.successful))

  implicit val resultArb: Arbitrary[EligibilityCheckResult] = Arbitrary(for {
    result ← Gen.alphaStr
    resultCode ← Gen.choose(1, 10)
    reason ← Gen.alphaStr
    reasonCode ← Gen.choose(1, 10)
  } yield EligibilityCheckResult(result, resultCode, reason, reasonCode))

  "check eligibility" must {
    val nino = randomNINO()

    lazy val urlWithoutUC = connector.url(nino, None)
      def urlWithUC(withinThreshold: Option[Boolean] = Some(true)) = connector.url(nino, Some(UCResponse(true, withinThreshold)))

    "return with the eligibility check result unchanged from ITMP" in {
      forAll { result: EligibilityCheckResult ⇒
        mockGet(urlWithoutUC)(Some(HttpResponse(200, Some(Json.toJson(result))))) // scalastyle:ignore magic.number
        Await.result(connector.isEligible(nino).value, 5.seconds) shouldBe Right(Some(result))
      }
    }

    "pass the UC params to DES if they are provided" in {
      forAll { result: EligibilityCheckResult ⇒
        mockGet(urlWithUC())(Some(HttpResponse(200, Some(Json.toJson(result))))) // scalastyle:ignore magic.number
        Await.result(connector.isEligible(nino, Some(UCResponse(true, Some(true)))).value, 5.seconds) shouldBe Right(Some(result))
      }
    }

    "do not pass the UC withinThreshold param to DES if its not set" in {
      forAll { result: EligibilityCheckResult ⇒
        mockGet(urlWithUC(None))(Some(HttpResponse(200, Some(Json.toJson(result))))) // scalastyle:ignore magic.number
        Await.result(connector.isEligible(nino, Some(UCResponse(true, None))).value, 5.seconds) shouldBe Right(Some(result))
      }
    }

    "handle errors when parsing invalid json" in {
      inSequence {
        mockGet(urlWithoutUC)(Some(HttpResponse(200, Some(Json.toJson("""{"invalid": "foo"}"""))))) // scalastyle:ignore magic.number
        // WARNING: do not change the message in the following check - this needs to stay in line with the configuration in alert-config
        mockPagerDutyAlert("Could not parse JSON in eligibility check response")
      }

      Await.result(connector.isEligible(nino).value, 5.seconds).isLeft shouldBe true
    }

    "handle 404 responses when nino is not found when an eligibility check is made" in {
      inSequence {
        mockGet(urlWithoutUC)(Some(HttpResponse(404, None))) // scalastyle:ignore magic.number
      }

      Await.result(connector.isEligible(nino).value, 5.seconds) shouldBe Right(None)
    }

    "return with an error" when {
      "the call fails" in {
        inSequence {
          mockGet(urlWithoutUC)(None)
          // WARNING: do not change the message in the following check - this needs to stay in line with the configuration in alert-config
          mockPagerDutyAlert("Failed to make call to check eligibility")
        }

        Await.result(connector.isEligible(nino).value, 5.seconds).isLeft shouldBe true
      }

      "the call comes back with an unexpected http status" in {
        forAll { status: Int ⇒
          whenever(status > 0 && status =!= 200 && status =!= 404) {
            inSequence {
              mockGet(urlWithoutUC)(Some(HttpResponse(status)))
              // WARNING: do not change the message in the following check - this needs to stay in line with the configuration in alert-config
              mockPagerDutyAlert("Received unexpected http status in response to eligibility check")
            }

            Await.result(connector.isEligible(nino).value, 5.seconds).isLeft shouldBe true
          }

        }

      }

    }

  }
}

