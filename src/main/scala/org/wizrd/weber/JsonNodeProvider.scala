package org.wizrd.weber

import java.lang.reflect.Type
import javax.ws.rs.core.{MediaType, MultivaluedMap}
import javax.ws.rs.ext.{MessageBodyReader, MessageBodyWriter}
import org.codehaus.jackson.JsonNode
import java.io.{InputStream, InputStreamReader, OutputStream, OutputStreamWriter}
import java.lang.annotation.Annotation
import com.google.inject.Singleton
import javax.ws.rs.Produces
import javax.ws.rs.ext.Provider
import com.codahale.jerkson.AST._
import com.codahale.jerkson.Json
import com.google.inject.Inject
import org.codehaus.jackson.map.ObjectMapper

@Provider @Produces(Array("application/json")) @Singleton
class JsonNodeProvider @Inject()(val om:ObjectMapper)
      extends MessageBodyWriter[JsonNode] with MessageBodyReader[JsonNode] {
  def getSize(node:JsonNode, classType:Class[_], genericType:Type,
              annotations:Array[Annotation], mediaType:MediaType) = -1l

  def isWriteable(classType:Class[_], genericType:Type,
                  annotations:Array[Annotation], mediaType:MediaType) = {
    classOf[JsonNode].isAssignableFrom(classType) &&
    mediaType.equals(MediaType.APPLICATION_JSON_TYPE)
  }

  def writeTo(node:JsonNode, classType:Class[_], genericType:Type,
              annotations:Array[Annotation], mediaType:MediaType,
              headers:MultivaluedMap[String,Object], out:OutputStream) = {
    om.writeValue(new OutputStreamWriter(out, "UTF-8"), node)
  }

  def isReadable(classType:Class[_], genericType:Type,
                 annotations:Array[Annotation], mediaType:MediaType) = {
    classOf[JsonNode].isAssignableFrom(classType) &&
    mediaType.equals(MediaType.APPLICATION_JSON_TYPE)
  }

  def readFrom(classType:Class[JsonNode], genericType:Type,
               annotations:Array[Annotation], mediaType:MediaType,
               headers:MultivaluedMap[String,String], in:InputStream) = {
    om.readTree(new InputStreamReader(in, "UTF-8"))
  }
}

@Provider @Produces(Array("application/json")) @Singleton
class JValueProvider extends MessageBodyWriter[JValue] with MessageBodyReader[JValue] {
  def getSize(node:JValue, classType:Class[_], genericType:Type,
              annotations:Array[Annotation], mediaType:MediaType) = -1l

  def isWriteable(classType:Class[_], genericType:Type,
                  annotations:Array[Annotation], mediaType:MediaType) = {
    classOf[JValue].isAssignableFrom(classType) &&
    mediaType.equals(MediaType.APPLICATION_JSON_TYPE)
  }

  def writeTo(node:JValue, classType:Class[_], genericType:Type,
              annotations:Array[Annotation], mediaType:MediaType,
              headers:MultivaluedMap[String,Object], out:OutputStream) = {
    Json.generate(node, out)
  }

  def isReadable(classType:Class[_], genericType:Type,
                 annotations:Array[Annotation], mediaType:MediaType) = {
    classOf[JValue].isAssignableFrom(classType) &&
    mediaType.equals(MediaType.APPLICATION_JSON_TYPE)
  }

  def readFrom(classType:Class[JValue], genericType:Type,
               annotations:Array[Annotation], mediaType:MediaType,
               headers:MultivaluedMap[String,String], in:InputStream) = {
    Json.parse[JValue](in)
  }
}
