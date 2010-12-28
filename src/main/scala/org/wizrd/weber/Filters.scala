package org.wizrd.weber

import com.sun.jersey.spi.container.{ContainerRequest, ContainerResponse, ContainerResponseFilter}
import java.io.File
import javax.activation.MimetypesFileTypeMap
import javax.ws.rs.core.Response
import scala.collection.JavaConversions._

class CorsHeaderResponseFilter extends ContainerResponseFilter {
  override def filter(request:ContainerRequest, response:ContainerResponse) = {
    // don't like always sending this header, but it seems impossible to detect
    // when to send it and when not to
    response.getHttpHeaders.add("Access-Control-Allow-Origin", "*")

    val requestedMethods = request.getRequestHeader("Access-Control-Request-Method")
    if (requestedMethods != null && !requestedMethods.isEmpty) {
      response.getHttpHeaders.add("Access-Control-Allow-Methods", requestedMethods.mkString(","))
    }

    val requestedHeaders = request.getRequestHeader("Access-Control-Request-Headers")
    if (requestedHeaders != null && !requestedHeaders.isEmpty) {
      response.getHttpHeaders.add("Access-Control-Allow-Headers", requestedHeaders.mkString(","))
    }

    // Hack because Chrome (& other browsers?) keep the location header
    // protected, even though that doesn't make sense... see:
    // http://code.google.com/p/chromium/issues/detail?id=58487
    // http://www.mail-archive.com/public-webapps@w3.org/msg08728.html
    if (response.getStatus == Response.Status.CREATED.getStatusCode &&
        response.getEntity == null) {
      val location = response.getHttpHeaders.getFirst("Location")
      if (location != null) {
        response.setEntity(location)
      }
    }

    response
  }
}

class StaticContentResponseFilter extends ContainerResponseFilter {
  val mimeTypes = new MimetypesFileTypeMap();
  val dir = new File("src/main/webapp")

  override def filter(request:ContainerRequest, response:ContainerResponse) = {
    if (response.getStatus == Response.Status.NOT_FOUND.getStatusCode) {
      val path = request.getPath
      val file = new File(dir, path)

      if (file.exists) {
        response.setResponse(Response.ok(file, mimeTypes.getContentType(path)).build())
      }
    }

    response
  }
}
