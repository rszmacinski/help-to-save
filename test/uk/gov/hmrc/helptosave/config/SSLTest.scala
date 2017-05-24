package uk.gov.hmrc.helptosave.config

import java.io.{File, FileInputStream, InputStream}
import java.security.KeyStore
import java.util.Properties

import org.apache.http.conn.ssl.{SSLConnectionSocketFactory, SSLSocketFactory}
import org.apache.http.entity.ContentType
import io.restassured.config.SSLConfig
import org.scalatest.FunSuite

/**
  * Created by jackie on 23/05/17.
  */
class SSLTest extends FunSuite {

  //val keyStore = KeyStore.getInstance("PKCS12")

  //val certKeyStoreLocation = new FileInputStream(new File("src/test/resources/keystores/ca-chain.cert.jks"))
  //val trustStore = KeyStore.getInstance("jks")
 // trustStore.load(certKeyStoreLocation, "changeit".toCharArray())


  //val clientAuthFactory = new org.apache.http.conn.ssl.SSLSocketFactory(keyStore, "changeit", trustStore)
  //clientAuthFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)

  //val config = new SSLConfig().with().sslSocketFactory(clientAuthFactory).and().allowAllHostnames()


  test("check there is a NSI certificate in keystore") {
    val ks = KeyStore.getInstance(KeyStore.getDefaultType())
    val fis: FileInputStream = new FileInputStream("/usr/lib/jvm/java-8-oracle/jre/lib/security/cacerts")
    ks.load(fis, "changeit".toCharArray)
    assert(ks.containsAlias("vdc.tools.tax.service.gov.uk"))
  }

  test("hit the nsi end point to generate a stack trace") {
    val url = ""
  }


}
