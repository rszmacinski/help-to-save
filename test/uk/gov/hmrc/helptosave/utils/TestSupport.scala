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

package uk.gov.hmrc.helptosave.utils

import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpecLike}
import uk.gov.hmrc.helptosave.config.WSHttp
import uk.gov.hmrc.play.http.HeaderCarrier
import java.time.LocalDate

import org.scalacheck.Gen
import hmrc.smartstub._
import uk.gov.hmrc.domain.Generator

trait TestSupport extends WordSpecLike with Matchers with MockFactory {

  implicit val ec = scala.concurrent.ExecutionContext.global

  implicit val hc = HeaderCarrier()

  val mockHttp = mock[WSHttp]

  private val hmrcGenerator = new Generator()

  val startDate = LocalDate.of(1800,1,1) // scalastyle:ignore magic.number
  val endDate = LocalDate.of(2000,1,1) // scalastyle:ignore magic.number
  val dateGen: Gen[LocalDate] = Gen.date(startDate, endDate)

  def randomDate() = dateGen.sample.getOrElse(sys.error("Could not generate date"))

  def randomNINO() = hmrcGenerator.nextNino.value
}
