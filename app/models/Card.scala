package models

import anorm._
import anorm.SqlParser._
import models._
import models.forms._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import akka.util.HashCode
import forms.FormCard
import scala.language.postfixOps
import scala.util.Random
import play.api.Logger

/**
 * Cards models an idea created by a ThinkingSession participant in the course of the 6 Hats process.
 * Every cards holds information about the user (creator), the hat in which it was created, the ThinkingSession
 * it belongs to and of course the content (for now only text)
 * @author Nemo
 */
case class Card(
  id: Long,
  thinkingSession: ThinkingSession,
  content: String,
  hat: Hat,
  creator: User) {

  def toJson(): JsObject = {
    Json.obj(
      "id" -> this.id,
      "hat" -> this.hat.name.toLowerCase,
      "thinkingSessionId" -> this.thinkingSession.id,
      "content" -> this.content,
      "username" -> this.creator.name)
  }
}

object Card {

  val dummy: Card = Card(1, ThinkingSession.dummy, "dfsfafsd", Hat.dummy, User.dummy);

  /**
   * ORM simple
   */
  //as(get[Option[String]]("something") ?).getOrElse(None)
  val DBParser = {
    get[Long]("id") ~
      get[Long]("thinking_session") ~
      get[String]("content") ~
      get[Long]("hat") ~
      get[Long]("creator") map {
        case id ~ thinkingSessionId ~ content ~ hatId ~ creatorId =>
          Card(id, ThinkingSession.byId(thinkingSessionId).get, content, Hat.byId(hatId).get,
            User.byId(creatorId).get);
      }
  }

  /**
   * Get all Cards (guess we'll never need this...)
   */
  def all(): List[Card] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM card ORDER BY time ASC").as(Card.DBParser *)
    }
  }

  /**
   * Get all cards of a given ThinkingSession
   */
  def byThinkingSession(thinkingSession: ThinkingSession): List[Card] = {
    byThinkingSession(thinkingSession.id)
  }

  def byThinkingSession(thinkingSessionId: Long): List[Card] = {
    DB.withConnection { implicit connection =>
      SQL("""
          select * 
          from card 
          where thinking_session={id} 
          ORDER BY time ASC
          """).on(
        'id -> thinkingSessionId).as(Card.DBParser *)
    }
  }

  /**
   * Get all cards created by a user
   */
  def byUser(user: User): List[Card] = {
    byUser(user.id)
  }

  def byUser(userId: Long): List[Card] = {
    DB.withConnection { implicit connection =>
      SQL("""
          SELECT * 
          FROM card 
          WHERE creator = {id} 
          ORDER BY time ASC
          """).on(
        'id -> userId).as(Card.DBParser *)
    }
  }

  /**
   * Get all cards created by a user in a specific session
   */
  def byUserInSession(user: User, session: ThinkingSession): List[Card] = {
    byUserInSession(user.id, session.id)
  }

  def byUserInSession(userId: Long, sessionId: Long): List[Card] = {
    DB.withConnection { implicit connection =>
      SQL("""
          SELECT * 
          FROM card 
          WHERE creator = {userId} and thinking_session ={sessionId} 
          ORDER BY time ASC
          """).on(
        'userId -> userId,
        'sessionId -> sessionId).as(Card.DBParser *)
    }
  }

  def byId(id: Long): Option[Card] = {
    DB.withConnection { implicit connection =>
      SQL("select * from card where id = {id}").on(
        'id -> id).as(Card.DBParser *).headOption
    }
  }

  /**
   * Create a new card.
   * This will return the id of the created card
   */
  def create(content: String, thinkingSessionId: Long, hatId: Long, creatorId: Long): Long = {
    DB.withConnection { implicit connection =>
      // create unique id by hashing contents + timestamp
      val id: Long = (content + thinkingSessionId + hatId + creatorId + Random.nextLong + System.currentTimeMillis).hashCode
      val sql = SQL(
        """
          INSERT INTO card (id,content,thinking_session,hat,creator) 
          VALUES ({id},{content},{thinkingSessionId},{hat},{creatorId})
          """).on(
          'id -> id,
          'content -> content,
          'thinkingSessionId -> thinkingSessionId,
          'hat -> hatId,
          'creatorId -> creatorId)
      sql.executeUpdate();
      id
    }
  }

  /**
   * Create a new card.
   * This will return the id of the created card
   */
  def create(content: String, thinkingSession: ThinkingSession, hat: Hat, creator: User): Long = {
    create(content, thinkingSession.id, hat.id, creator.id)
  }

  /**
   * Delete a card (if it was shit or something...)
   */
  def delete(card: Card): Int = {
    delete(card.id)
  }

  def delete(cardId: Long): Int = {
    DB.withConnection { implicit connection =>
      SQL("delete from card where id = {id}").on(
        'id -> cardId).executeUpdate()
    }
  }

  def listToJson(cards: List[Card]) = {
    Json.toJson(cards.map(card => card.toJson()))
  }

  def byOnlyInSession(sessionId: Long): List[Long] = {
    DB.withConnection { implicit connection =>
      SQL("""select DISTINCT creator from card where thinking_session ={sessionId}""").on(
        'sessionId -> sessionId).as(long("creator") map { case creator => creator } *)
    }
  }

  def byCardsforUser(sessionId: Long, hatID: Long, creatorID: Long): Long = {
    DB.withConnection { implicit connection =>
      SQL("""select count(card.id) as cNO  from card where thinking_session ={sessionId} and creator = {creatorID} and card.hat = {hatID}""").on(
        'sessionId -> sessionId,
        'creatorID -> creatorID,
        'hatID -> hatID).as(get[Long]("cNO").single)
    }
  }

  def addToBucket(cardId: Long, bucketId: Long): Int = {
    DB.withConnection { implicit connection =>
      SQL("""
          update card
          set bucket = {bucketId}
          where id = {cardId}
          """).on(
        'cardId -> cardId,
        'bucketId -> bucketId).executeUpdate()
    }

  }

}