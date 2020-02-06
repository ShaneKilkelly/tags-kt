package com.overleaf.tags

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import com.fasterxml.jackson.databind.*
import io.ktor.jackson.*
import io.ktor.features.*
import io.ktor.client.*

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters.*;
import com.mongodb.client.model.Updates.*;
import org.bson.Document
import org.bson.types.ObjectId

import java.util.Arrays


data class CreateTagBody(val name: String)
data class UpdateTagUserIdsBody(val user_id: String)
data class RenameTagBody(val name: String)

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

  install(ContentNegotiation) {
      jackson {
          enable(SerializationFeature.INDENT_OUTPUT)
      }
  }

  // Mongo stuff
  val connection = "mongodb://mongo:27017"
  // val connection = "mongodb://localhost:27018"
  val mongoClient = MongoClients.create(connection)
  val db = mongoClient.getDatabase("sharelatex")
  val tags = db.getCollection("tags")

  routing {

    get("/") {
      call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
    }

    get("/status") {
      call.respondText("tags sharelatex up", contentType = ContentType.Text.Plain)
    }

    get("/health_check") {
      call.respondText("", contentType = ContentType.Text.Plain)
    }

    get("/user/{id}/tag") {
      val id = call.parameters["id"]
      val result = ArrayList<Map<String, Any>>()
      tags.find(eq("user_id", id))
        .forEach {
          val asMap: Map<String, Any> = mongoDocumentToMap(it)
          result.add(asMap)
        }
      call.respond(result)
    }

    post("/user/{id}/tag") {
      val id = call.parameters["id"]
      val body = call.receive<CreateTagBody>()
      val doc = Document()
        .append("name", body.name)
        .append("user_id", id)
        .append("project_ids", Arrays.asList<String>())
      tags.insertOne(doc)
      call.respond(mongoDocumentToMap(doc))
    }

    put("/user/{id}/tag") {
      val id = call.parameters["id"]
      val body = call.receive<UpdateTagUserIdsBody>()
      tags.updateMany(
        eq("user_id", id),
        set("user_id", body.user_id)
      )
      call.response.status(HttpStatusCode.NoContent)
      call.respondText("")
    }

    post("/user/{id}/tag/{tag}/rename") {
      val id = call.parameters["id"]
      val tag = call.parameters["tag"]
      val body = call.receive<RenameTagBody>()
      tags.updateOne(
        and(eq("user_id", id), eq("_id", ObjectId(tag))),
        set("name", body.name)
      )
      call.response.status(HttpStatusCode.NoContent)
      call.respondText("")
    }

    delete("/user/{id}/tag/{tag}") {
      val id = call.parameters["id"]
      val tag = call.parameters["tag"]
      tags.deleteOne(
        and(
          eq("user_id", id),
          eq("_id", ObjectId(tag))
        )
      )
      call.response.status(HttpStatusCode.NoContent)
      call.respondText("")
    }
  }
}
