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

import cats.instances.list._
import cats.instances.option._
import cats.syntax.traverse._
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.PrivilegedApplication
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals.allEnrolments
import uk.gov.hmrc.helptosave.util.{Logging, toFuture}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StrideAuth(htsAuthConnector: AuthConnector)
  extends BaseController with AuthorisedFunctions with ServicesConfig with Logging {

  override def authConnector: AuthConnector = htsAuthConnector

  private val requiredRoles: List[String] = runModeConfiguration.underlying.getStringList("stride.roles").asScala.toList

  def authorisedFromStride(action: Request[AnyContent] ⇒ Future[Result]): Action[AnyContent] =
    Action.async { implicit request ⇒
      authorised(AuthProviders(PrivilegedApplication)).retrieve(allEnrolments) {
        enrolments ⇒
          val necessaryRoles: Option[List[Enrolment]] =
            requiredRoles.map(enrolments.getEnrolment).traverse[Option, Enrolment](identity)

          necessaryRoles.fold[Future[Result]](Unauthorized("Insufficient roles")) { _ ⇒ action(request) }
      }.recover {
        case _: NoActiveSession ⇒
          logger.warn("user is not logged in via stride, probably a hack?")
          Forbidden("no stride session found for logged in user")
      }
    }
}
