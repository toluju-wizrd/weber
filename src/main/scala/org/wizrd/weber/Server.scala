package org.wizrd.weber

import org.wizrd.util.Logging
import org.wizrd.util.guice.Helpers
import org.wizrd.util.guice.ScalaModule._

import javax.ws.rs.ext.{ContextResolver, Provider}
import javax.ws.rs.Produces
import org.codehaus.jackson.jaxrs.JacksonJsonProvider
import org.codehaus.jackson.map.ObjectMapper
import org.mortbay.jetty.servlet.Context
import org.mortbay.jetty.{Server => JettyServer}
import com.google.inject.{Injector, Scopes}
import com.google.inject.binder.AnnotatedBindingBuilder
import com.google.inject.servlet.{GuiceFilter, GuiceServletContextListener, ServletModule}
import com.sun.jersey.api.core.ResourceConfig
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer
import com.sun.jersey.spi.container.{ContainerRequestFilter, ContainerResponseFilter}
import java.net.InetAddress
import org.mortbay.servlet.GzipFilter
import javax.servlet.{GenericServlet, ServletRequest, ServletResponse}
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuilder
import com.google.inject.{Inject, Singleton}


object Server extends Logging {
  def start(injector:Injector, port:Int = 8080) = {
    val server = new JettyServer(port)
    val root = new Context(server, "/", Context.SESSIONS)

    root.addEventListener(new GuiceServletContextListener() {
      override def getInjector = injector
    })

    root.addFilter(classOf[GuiceFilter], "/*", 0)
    root.addServlet(classOf[EmptyServlet], "/*")

    log.info("Server starting")
    server.start
    val host = InetAddress.getLocalHost.getHostName
    log.info("Server started on http://%s:%d", host, port)
  }
}


class EmptyServlet extends GenericServlet {
  def service(req:ServletRequest, resp:ServletResponse) = {
    throw new IllegalStateException("Jersey fell through to servlet")
  }
}

abstract class WeberModule(val useGzip:Boolean = true, val useWADL:Boolean = false)
         extends ServletModule with Logging {

  override def logClass = classOf[WeberModule]

  override def configureServlets = {
    configureWeber()

    // Scopes matter, be careful!

    bind[ObjectMapper].in(Scopes.SINGLETON)
    bind[ObjectMapperProvider]
    bind[JacksonJsonProvider].in(Scopes.SINGLETON)
    bind[JsonNodeProvider]
    bind[JValueProvider]

    if (useGzip) {
      bind[GzipFilter].in(Scopes.SINGLETON)
      filter("*") through (classOf[GzipFilter])
    }

    val requestFilters = requestFilterBuilder.result
    val responseFilters = responseFilterBuilder.result

    if (!requestFilters.isEmpty)
      log.info("Adding ContainerRequestFilters: %s", requestFilters.map(_.getName).mkString(","))
    if (!responseFilters.isEmpty)
      log.info("Adding ContainerResponseFilters: %s", responseFilters.map(_.getName).mkString(","))

    serve("*") `with` (classOf[GuiceContainer], Map(
      ResourceConfig.FEATURE_DISABLE_WADL ->
        (if (useWADL) "false" else "true"),
      ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS ->
        requestFilters.map(_ getName).mkString(","),
      ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS ->
        responseFilters.map(_ getName).mkString(",")
    ).asJava)
  }

  protected def bind[T](implicit m:Manifest[T]):AnnotatedBindingBuilder[T] = {
    m.typeArguments match {
      case Nil => bind(m.erasure.asInstanceOf[Class[T]])
      case _   => bind(Helpers.typeLiteral(m))
    }
  }

  private val requestFilterBuilder = ArrayBuilder.make[Class[_<:ContainerRequestFilter]]
  private val responseFilterBuilder = ArrayBuilder.make[Class[_<:ContainerResponseFilter]]
  
  def addRequestFilter[T<:ContainerRequestFilter](implicit m:Manifest[T]) = {
    requestFilterBuilder += m.erasure.asInstanceOf[Class[_<:ContainerRequestFilter]]
  }
  
  def addResponseFilter[T<:ContainerResponseFilter](implicit m:Manifest[T]) = {
    responseFilterBuilder += m.erasure.asInstanceOf[Class[_<:ContainerResponseFilter]]
  }

  def configureWeber()
}

@Provider @Singleton @Produces(Array("application/json"))
class ObjectMapperProvider @Inject()(val mapper:ObjectMapper) extends ContextResolver[ObjectMapper] {
  override def getContext(clazz:Class[_]) = mapper
}
