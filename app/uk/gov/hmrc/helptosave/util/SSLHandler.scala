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

package uk.gov.hmrc.helptosave.util

import java.io.FileInputStream
import java.security.cert.X509Certificate
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl._

import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.commons.io.IOUtils
import org.apache.http.ssl.SSLContexts
import uk.gov.hmrc.helptosave.WSHttpProxy
import uk.gov.hmrc.play.config.ServicesConfig


object SSLHandler extends ServicesConfig {

  def message(key:String):String = {
    s"[SSLHandler][NotFound] help-to-save.$key"
  }

  lazy val keyStoreFileName = getConfString("help-to-save.keystore",throw new RuntimeException(message("keystore")))
  lazy val keyStorePassword = getConfString("help-to-save.password", throw new RuntimeException(message("password")))
  //not sure if these should be help-to-save.keystore or nsi.keystore???

  def httpClientFactory: WSHttpProxy = {
    val httpClientBuilder = HttpClients.custom()
    val httpProxy = new WSHttpProxy

    val ks = KeyStore.getInstance(KeyStore.getDefaultType)
    val keyStorePath = getClass.getClassLoader.getResource(keyStoreFileName)
    val inputStream = new FileInputStream(keyStorePath.getPath)
    ks.load(inputStream, keyStorePassword.toArray)
    IOUtils.closeQuietly(inputStream)
    // create trust manager from keystore
    val tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
    tmf.init(ks)
    val trustManager = tmf.getTrustManagers
    // associate trust manager with the httpClient
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(Array(), trustManager, null)
    httpClientBuilder.setSSLContext(sslContext)

    httpProxy

    // ending httpClient creation
    ////httpClientBuilder.build()
  }

}
