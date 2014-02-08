package models

import anorm._
import anorm.SqlParser._
import play.api.db._
import play.api.Play.current
import scala.util.Random
import play.api.libs.json._

case class Bucket(id: Long, sessionId: Long, name: String) {
  def asJson: JsObject = Json.obj(
    "bucketId" -> id,
    "sessionId" -> sessionId,
    "name" -> name)

}

object Bucket {

  val DBParser = {
    get[Long]("id") ~
      get[Long]("thinking_session") ~
      get[String]("name") map {
        case id ~ sessionId ~ name => Bucket(id, sessionId, name)
      }
  }

  def byId(id: Long): Option[Bucket] = {
    DB.withConnection { implicit connection =>
      SQL("""
          select * 
          from bucket 
          where id = {id}
          """).on(
        'id -> id).as(Bucket.DBParser *).headOption
    }
  }

  def saveName(name: String, id: Long) = {
    DB.withConnection { implicit connection =>
      SQL("""
          update bucket
          set name = {name}
          where id = {bucketId}
          """).on(
        'bucketId -> id,
        'name -> name).executeUpdate()
    }
  }

  def create(session: ThinkingSession): Long = create(session.id)

  def create(sessionId: Long): Long = {
    DB.withConnection { implicit connection =>
      val id: Long = (sessionId + Random.nextLong + System.currentTimeMillis).hashCode
      SQL("""
          insert into bucket (id,thinking_session) 
          values ({id},{sessionId})
          """).on(
        'id -> id,
        'sessionId -> sessionId).executeUpdate()
      id
    }
  }

  def addCard(bucketId: Long, cardId: Long): Int = {
    // handle adding of card to bucket in Card controller, because it manipulates the cards table  
    Card.addToBucket(cardId, bucketId);
  }
}