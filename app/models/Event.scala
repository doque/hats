package models;

import anorm._
import anorm.SqlParser._
import play.libs.Json
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.Date
import scala.collection.immutable.HashMap
import scala.collection.JavaConversions
import play.api.Play.current
import play.api.db.DB
import scala.util.Random
import play.api.libs.json.JsValue

case class Event(
  id: Long,
  eventType: String,
  thinkingSession: ThinkingSession,
  hat: Hat,
  user: Option[User],
  card: Option[Card],
  bucket: Option[Bucket],
  time: Date) {

  def asJson(): JsonNode = {
    if (eventType == null) {
      throw new IllegalArgumentException();
    }

    var result: ObjectNode = Json.newObject();
    result.put("eventType", this.eventType);
    result.put("eventData", Json.toJson(getEventData));
    return result;
  }

  def getEventData: java.util.Map[String, String] = {
    val baseData = HashMap[String, String](
      "thinkingSessionId" -> thinkingSession.id.toString,
      "hat" -> hat.name.toLowerCase,
      "time" -> time.getTime().toString(),
      "username" -> userName,
      "cardId" -> cardId,
      "bucketId" -> bucketId,
      "bucketName" -> bucketName,
      "content" -> cardContent)
    JavaConversions.mapAsJavaMap(baseData)
  }

  def cardId: String = card match {
    case Some(c) => c.id.toString
    case None => null
  }

  def bucketName: String = bucket match {
    case Some(b) => b.name
    case None => null
  }
  def bucketId: String = bucket match {
    case Some(b) => b.id.toString
    case None => null
  }

  def cardContent = card match {
    case Some(c) => c.content
    case None => null
  }

  def userName = user match {
    case Some(u) => u.name
    case None => null
  }

  val hasCard: Boolean = card match {
    case Some(_) => true
    case None => false
  }

  val hasUser: Boolean = user match {
    case Some(_) => true
    case None => false
  }
}

object EventType {
  val createSession = "createSession"
  val addCard = "addCard"
  val addBucket = "addBucket"
  val renameBucket = "renameBucket"
  val addCardToBucket = "addCardToBucket"
  val userJoin = "userJoin"
  val moveHat = "moveHat"
  val closeSession = "closeSession"
}

object Event {

  val dummy = Event(0, "dummyEvent", ThinkingSession.dummy, Hat.dummy, None, None, None, new Date())

  val DBParser = {
    get[Long]("id") ~
      get[String]("type") ~
      get[Long]("thinking_session") ~
      get[Long]("hat") ~
      (get[Long]("user") ?) ~
      (get[Long]("card") ?) ~
      (get[Long]("bucket") ?) ~
      get[Date]("time") map {
        case id ~ eventType ~ thinkingSessionId ~ hatId ~ userId ~ cardId ~ bucketId ~ time =>
          Event(id, eventType,
            ThinkingSession.byId(thinkingSessionId).get, Hat.byId(hatId).get,
            userId match { case Some(u) => User.byId(u) case None => None },
            cardId match { case Some(c) => Card.byId(c) case None => None },
            bucketId match { case Some(b) => Bucket.byId(b) case None => None },
            time);
      }
  }

  def create(eventType: String, thinkingSessionId: Long, hatId: Long, userId: Option[Long], cardId: Option[Long], bucketId: Option[Long], time: Date): Long = {
    DB.withConnection { implicit connection =>
      val id: Long = (eventType.hashCode() + thinkingSessionId + userId.hashCode() + cardId.hashCode() + time.getTime()).hashCode() + System.currentTimeMillis() + Random.nextLong
      val sql = SQL("""
          insert into event (id,type,thinking_session,hat,user,card,bucket,time) 
          values ({id},{eventType},{thinkingSessionId},{hatId},{userId},{cardId},{bucket},{time})
          """).on(
        'id -> id,
        'eventType -> eventType,
        'thinkingSessionId -> thinkingSessionId,
        'hatId -> hatId,
        'userId -> userId,
        'cardId -> cardId,
        'bucket -> bucketId,
        'time -> time)

      sql.executeUpdate()
      id
    }
  }

  def create(eventType: String, thinkingSession: ThinkingSession, hat: Hat, user: Option[User], card: Option[Card], bucket: Option[Bucket], time: Date): Long = {
    create(eventType, thinkingSession.id, hat.id, user.collect({ case u: User => u.id }),
      card.collect({ case c: Card => c.id }), bucket.collect({ case b: Bucket => b.id }), time)
  }

  def byId(id: Long): Option[Event] = {
    DB.withConnection { implicit connection =>
      SQL("select * from event where id = {id}").on(
        'id -> id).as(DBParser *).headOption
    }
  }

  def byThinkingSession(thinkingSession: ThinkingSession): List[Event] = {
    byThinkingSessionId(thinkingSession.id)
  }

  def byThinkingSessionId(thinkingSessionId: Long): List[Event] = {
    DB.withConnection { implicit connection =>
      SQL("select * from event where thinking_session={id}").on(
        'id -> thinkingSessionId).as(DBParser *)
    }
  }

  def byUser(user: User): List[Event] = {
    byUser(user.id)
  }

  def byUser(userId: Long): List[Event] = {
    DB.withConnection { implicit connection =>
      SQL("select * from event where creator = {id}").on(
        'id -> userId).as(DBParser *)
    }
  }
}

