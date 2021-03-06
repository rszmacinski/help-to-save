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

import java.util.UUID

import cats.data.EitherT
import com.google.inject.{ImplementedBy, Inject, Singleton}
import play.api.http.Status
import play.mvc.Http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.helptosave.config.WSHttp
import uk.gov.hmrc.helptosave.models.{ErrorResponse, NSIUserInfo, UCResponse}
import uk.gov.hmrc.helptosave.util.HttpResponseOps._
import uk.gov.hmrc.helptosave.util.Logging.LoggerOps
import uk.gov.hmrc.helptosave.util.{Logging, NINOLogMessageTransformer, Result, base64Encode}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[HelpToSaveProxyConnectorImpl])
trait HelpToSaveProxyConnector {

  def createAccount(userInfo: NSIUserInfo)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse]

  def ucClaimantCheck(nino: String, txnId: UUID)(implicit hc: HeaderCarrier, ec: ExecutionContext): Result[UCResponse]
}

@Singleton
class HelpToSaveProxyConnectorImpl @Inject() (http: WSHttp)(implicit transformer: NINOLogMessageTransformer)
  extends HelpToSaveProxyConnector with ServicesConfig with Logging {

  val proxyURL: String = baseUrl("help-to-save-proxy")

  override def createAccount(userInfo: NSIUserInfo)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    http.post(s"$proxyURL/help-to-save-proxy/create-account", userInfo)
      .recover {
        case e ⇒
          logger.warn(s"unexpected error from proxy during /create-de-account, message=${e.getMessage}")
          val errorJson = ErrorResponse("unexpected error from proxy during /create-de-account", s"${e.getMessage}").toJson()
          HttpResponse(INTERNAL_SERVER_ERROR, responseJson = Some(errorJson))
      }
  }

  override def ucClaimantCheck(nino: String, txnId: UUID)(implicit hc: HeaderCarrier, ec: ExecutionContext): Result[UCResponse] = {
    val url = s"$proxyURL/help-to-save-proxy/uc-claimant-check?nino=${base64Encode(nino)}&transactionId=$txnId"

    EitherT[Future, String, UCResponse](
      http.get(url).map {
        response ⇒
          logger.info(s"response body from UniversalCredit check is: ${response.body}", nino)
          response.status match {
            case Status.OK ⇒
              val result = response.parseJson[UCResponse]
              result.fold(
                e ⇒ logger.warn(s"Could not parse UniversalCredit response, received 200 (OK), error=$e", nino),
                _ ⇒ logger.info(s"Call to check UniversalCredit check is successful, received 200 (OK)", nino)
              )
              result

            case other ⇒
              logger.warn(s"Call to check UniversalCredit check unsuccessful. Received unexpected status $other", nino)
              Left(s"Received unexpected status($other) from UniversalCredit check")
          }
      }.recover {
        case e ⇒
          Left(s"Call to UniversalCredit check unsuccessful: ${e.getMessage}")
      }
    )
  }
}
