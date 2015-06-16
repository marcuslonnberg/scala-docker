package se.marcuslonnberg.scaladocker.remote.api

import java.io.{File, FileInputStream}
import java.security.cert.{Certificate, CertificateFactory}
import java.security.spec.PKCS8EncodedKeySpec
import java.security.{KeyFactory, KeyStore, PrivateKey, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import org.apache.commons.ssl.PKCS8Key

object Tls {
  def fromDir(dir: String) = {
    val caCert = {
      val caFile = new File(dir, "ca.pem")
      if (caFile.isFile) Some(caFile.getPath)
      else None
    }
    Tls(cert = dir + "/cert.pem", key = dir + "/key.pem", caCert = caCert)
  }
}

case class Tls(cert: String, key: String, caCert: Option[String] = None)

trait TlsSupport {
  private[api] def createSSLContext(tls: Tls): SSLContext = {
    val algorithm = "SunX509"
    val protocol = "TLSv1"
    val emptyPassword = "".toCharArray

    val privateKey = readPrivateKey(tls.key)
    val cert = readCert(tls.cert)
    val maybeCaCert = tls.caCert.map(readCert)

    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType)
    keyStore.load(null, null)

    keyStore.setEntry("key", new KeyStore.PrivateKeyEntry(privateKey, cert), new KeyStore.PasswordProtection(emptyPassword))

    val trustManager = TrustManagerFactory.getInstance(algorithm)
    maybeCaCert.foreach { caCert =>
      keyStore.setCertificateEntry("cacert", caCert.head)
    }
    trustManager.init(keyStore)

    val keyManager = KeyManagerFactory.getInstance(algorithm)
    keyManager.init(keyStore, emptyPassword)

    val context = SSLContext.getInstance(protocol)
    context.init(keyManager.getKeyManagers, trustManager.getTrustManagers, new SecureRandom)
    context
  }

  private[api] def readCert(certificateFilename: String): Array[Certificate] = {
    val certificateStream = new FileInputStream(certificateFilename)
    val certificateFactory = CertificateFactory.getInstance("X.509")
    val certs = certificateFactory.generateCertificates(certificateStream)

    var chain = new Array[Certificate](certs.size())
    chain = certs.toArray(chain)
    certificateStream.close()
    chain
  }

  private[api] def readPrivateKey(privateKeyFilename: String): PrivateKey = {
    val fileStream = new FileInputStream(privateKeyFilename)
    val key = new PKCS8Key(fileStream, null)
    val encodedKey = key.getDecryptedBytes

    val algorithm = if (key.isRSA) "RSA" else "DSA"
    val keyFactory = KeyFactory.getInstance(algorithm)
    keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encodedKey))
  }
}

