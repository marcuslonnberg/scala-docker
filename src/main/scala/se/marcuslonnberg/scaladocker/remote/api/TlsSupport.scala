package se.marcuslonnberg.scaladocker.remote.api

import java.io.{File, FileInputStream}
import java.security.cert.{Certificate, CertificateFactory}
import java.security.spec.PKCS8EncodedKeySpec
import java.security.{KeyFactory, KeyStore, PrivateKey, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import org.apache.commons.ssl.PKCS8Key

object TlsConfig {
  def fromDir(dir: String) = {
    val caCert = {
      val caFile = new File(dir, "ca.pem")
      if (caFile.isFile) Some(caFile.getPath)
      else None
    }
    TlsConfig(cert = dir + "/cert.pem", key = dir + "/key.pem", caCert = caCert)
  }
}

case class TlsConfig(cert: String, key: String, caCert: Option[String] = None)

trait TlsSupport {
  protected def createSSLContext(tls: TlsConfig): SSLContext = {
    val algorithm = "SunX509"
    val protocol = "TLSv1"
    val emptyPassword = "".toCharArray

    val privateKey = readPrivateKey(tls.key)
    val cert = readCert(tls.cert)
    val maybeCaCert = tls.caCert.map(readCert)

    val keyManagers = {
      val keyStore = KeyStore.getInstance(KeyStore.getDefaultType)
      keyStore.load(null, null)
      keyStore.setEntry("key", new KeyStore.PrivateKeyEntry(privateKey, cert), new KeyStore.PasswordProtection(emptyPassword))

      val keyManager = KeyManagerFactory.getInstance(algorithm)
      keyManager.init(keyStore, emptyPassword)
      keyManager.getKeyManagers
    }

    val trustManagers = maybeCaCert.map { caCert =>
      val trustManager = TrustManagerFactory.getInstance(algorithm)
      val keyStore = KeyStore.getInstance(KeyStore.getDefaultType)
      keyStore.load(null, null)
      keyStore.setCertificateEntry("cacert", caCert.head)

      trustManager.init(keyStore)
      trustManager.getTrustManagers
    }

    val context = SSLContext.getInstance(protocol)
    context.init(keyManagers, trustManagers.orNull, new SecureRandom)
    context
  }

  protected def readCert(certificateFilename: String): Array[Certificate] = {
    val certificateStream = new FileInputStream(certificateFilename)
    val certificateFactory = CertificateFactory.getInstance("X.509")
    val certs = certificateFactory.generateCertificates(certificateStream)

    var chain = new Array[Certificate](certs.size())
    chain = certs.toArray(chain)
    certificateStream.close()
    chain
  }

  protected def readPrivateKey(privateKeyFilename: String): PrivateKey = {
    val fileStream = new FileInputStream(privateKeyFilename)
    val key = new PKCS8Key(fileStream, null)
    val encodedKey = key.getDecryptedBytes

    val algorithm = if (key.isRSA) "RSA" else "DSA"
    val keyFactory = KeyFactory.getInstance(algorithm)
    keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encodedKey))
  }
}

