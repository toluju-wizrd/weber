package org.wizrd.weber

import org.wizrd.util.Logger

import com.google.inject.Guice
import javax.ws.rs.{Path, Produces, GET}
import org.codehaus.jackson.node.JsonNodeFactory.instance._

object Example {
  def main(args:Array[String]) = {
    Logger.init()

    val module = new WeberModule() {
      override def configureWeber() = {
        addResponseFilter[CorsHeaderResponseFilter]
        bind[ExampleResource]
      }
    }

    val injector = Guice.createInjector(module)

    Server.start(injector, 8181)
  }
}

@Path("/")
class ExampleResource {
  @GET @Produces(Array("text/plain"))
  def hello = "hello world"

  @GET @Path("json") @Produces(Array("application/json"))
  def json = {
    val node = objectNode
    node.put("msg", "Hello World")
    node.put("val", 42)
    node
  }

  def getName = "bob"
  def getAge = 33

  @GET @Path("json2") @Produces(Array("application/json"))
  def json2 = this
}
