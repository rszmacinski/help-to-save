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

package uk.gov.hmrc.helptosave.utils

import cats.data.EitherT
import cats.instances.future._
import uk.gov.hmrc.helptosave.connectors.ITMPEnrolmentConnector
import uk.gov.hmrc.helptosave.controllers.EnrolmentBehaviour
import uk.gov.hmrc.helptosave.repo.EnrolmentStore
import uk.gov.hmrc.helptosave.util._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait TestEnrolmentBehaviour extends TestSupport {

  val enrolmentStore: EnrolmentStore = mock[EnrolmentStore]
  val itmpConnector: ITMPEnrolmentConnector = mock[ITMPEnrolmentConnector]
  val enrolmentBehaviour: EnrolmentBehaviour = mock[EnrolmentBehaviour]

  def mockEnrolmentStoreUpdate(nino: NINO, itmpFlag: Boolean)(result: Either[String, Unit]): Unit =
    (enrolmentStore.update(_: NINO, _: Boolean))
      .expects(nino, itmpFlag)
      .returning(EitherT.fromEither[Future](result))

  def mockEnrolmentStoreGet(nino: NINO)(result: Either[String, EnrolmentStore.Status]): Unit =
    (enrolmentStore.get(_: NINO))
      .expects(nino)
      .returning(EitherT.fromEither[Future](result))

  def mockITMPConnector(nino: NINO)(result: Either[String, Unit]): Unit =
    (itmpConnector.setFlag(_: NINO)(_: HeaderCarrier, _: ExecutionContext))
      .expects(nino, *, *)
      .returning(EitherT.fromEither[Future](result))

}
