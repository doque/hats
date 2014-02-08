package models

import scala.concurrent.Future
import scala.util.Random
import anorm._
import anorm.SqlParser._
import play.api.Play.current
import play.api.db._
import play.api.mvc.AsyncResult
import scala.language.postfixOps
import java.util.Date
import org.joda.time._
import play.Logger

/**
 * The ThinkingSession models a whole session of the process. Don't read the code, read the comments ;-D
 * @author Nemo
 */
case class ThinkingSession(id: Long, owner: User, title: String, currentHat: Hat) {
  def isOwner(user: User): Boolean = owner.id == user.id
}

object ThinkingSession {

  /**
   * id                    	integer NOT NULL DEFAULT nextval('thinking_session_id_seq') PRIMARY KEY,
   * owner					integer NOT NULL REFERENCES `user`(id),
   * title                 	varchar(255) NOT NULL,
   * current_hat			integer REFERENCES hat(id)
   */
  /**
   * ORM simple
   */
  val DBParser = {
    get[Long]("id") ~
      get[Long]("owner") ~
      get[String]("title") ~
      get[Long]("current_hat") map {
        case id ~ ownerId ~ title ~ hatId =>
          ThinkingSession(id, User.byId(ownerId).get, title, Hat.byId(hatId).get);
      }
  }

  /**
   * Retrieve all Thinking Sessions (not sure if we'll ever need this...)
   */
  def all(): List[ThinkingSession] = {
    DB.withConnection { implicit conn =>
      SQL("select * from thinking_session").as(ThinkingSession.DBParser *)
    }

  }

  /**
   * Load a specific Thinking Session from DB specified by given ID
   */
  def byId(id: Long): Option[ThinkingSession] = {
    DB.withConnection { implicit connection =>
      SQL("select * from thinking_session where id = {id}").on(
        'id -> id).as(ThinkingSession.DBParser *) headOption
    }
  }

  /**
   * Retrieve all sessions a specified user is owner of
   */
  def byOwner(owner: User): List[ThinkingSession] = {
    byOwner(owner.id)
  }

  def byOwner(ownerId: Long): List[ThinkingSession] = {
    DB.withConnection { implicit connection =>
      SQL("select * from thinking_session where owner = {ownerId}").on(
        'ownerId -> ownerId).as(ThinkingSession.DBParser *)
    }
  }

  /**
   * Create a new ThinkingSession. Only needed argument is the current hat (i.e. starting hat for a new session).
   * The sessions cards are not needed, because the Card Object/Table holds a foreign key to the
   * parent sessions and there is no nicer way to model the 1:n relations ship with anorm (afaik)
   * This will NOT return the created Thinking Session!
   */
  def create(owner: User, title: String, hat: Hat): Long = {
    create(owner.id, title, hat.id)
  }

  def nextId(): Long = {
    DB.withConnection { implicit connection =>
      SQL("SELECT thinking_session_id_seq.nextval;").apply().map {
        case Row(nextId: Long) => nextId
      } head
    }
  }

  def create(ownerId: Long, title: String, hatId: Long): Long = {
    val id = nextId()
    DB.withConnection { implicit connection =>
      SQL("insert into thinking_session (id,owner,title,current_hat) values ({id},{ownerId},{title},{hatId})").on(
        'id -> id,
        'ownerId -> ownerId,
        'title -> title,
        'hatId -> hatId).executeUpdate()
    }
    id
  }

  /**
   * Changes the current Hat of a thinking session in db. Caution: the input model obj will still have the
   * old hat state (as it is assumed to be an immutable obj). For further use requery the ThinkingSession
   * from db (e.g. by ThinkginSession.getById([session id]) )
   */
  def changeHatTo(session: ThinkingSession, newHat: Hat): Int = {
    changeHatTo(session.id, newHat.id)
  }

  def changeHatTo(sessionId: Long, newHatId: Long): Int = {
    DB.withConnection { implicit connection =>
      SQL("""
          update thinking_session
          set current_hat = {newHatId}
          where id = {sessionId}
          """).on(
        'newHatId -> newHatId,
        'sessionId -> sessionId).executeUpdate()
    }
  }

  /**
   * Delete a Thinking Session
   */
  def delete(session: ThinkingSession): Int = {
    delete(session.id)
  }

  def delete(sessionId: Long): Int = {
    DB.withConnection { implicit connection =>
      SQL("delete from card where id = {id}").on(
        'id -> sessionId).executeUpdate()
    }
  }

  def addUser(session: ThinkingSession, user: User): Long = {
    addUser(session.id, user.id)
  }

  /**
   * returns the join token for the user
   */
  def addUser(sessionId: Long, userId: Long): Long = {
    val createTime = (new Date()).getTime().toString()
    DB.withConnection { implicit connection =>
      val token = (sessionId + userId + (new Date()).getTime() + Random.nextLong).hashCode
      SQL("""
          insert into participating (thinking_session,user,token) 
          values ({sessionId},{userId},{token})
          """).on(
        'sessionId -> sessionId,
        'userId -> userId,
        'token -> token)
        .executeUpdate()
      Logger.debug("token = " + token + " for session " + sessionId)
      token
    }
  }

  def userIds(session: ThinkingSession): List[Long] = {
    userIds(session.id)
  }

  def userIds(sessionId: Long): List[Long] = {
    DB.withConnection { implicit connection =>
      SQL("""
          select user 
          from participating 
          where thinking_session = {sessionId}
          """").on(
        'sessionId -> sessionId)
        .as(get[Long]("user") *)
    }
  }

  def user(session: ThinkingSession): List[User] = {
    user(session.id)
  }

  def user(sessionId: Long): List[User] = {
    userIds(sessionId).map(id => User.byId(id)).filter(o => o != None).map(o => o.get)
  }

  def userCount(session: ThinkingSession): Int = {
    userCount(session.id)
  }

  def userCount(sessionId: Long): Int = {
    DB.withConnection { implicit connection =>
      SQL("""
          select count(*) as num
          from participating 
          where thinking_session = {sessionId}
          """").on(
        'sessionId -> sessionId)
        .as(get[Int]("num").single)
    }
  }

  def readyCount(session: ThinkingSession): Int = {
    readyCount(session.id)
  }

  def readyCount(sessionId: Long): Int = {
    DB.withConnection { implicit connection =>
      SQL("""
          select count(*) as num
          from participating 
          where thinking_session = {sessionId} and ready = 1
          """").on(
        'sessionId -> sessionId)
        .as(get[Int]("num").single)
    }
  }

  /**
   * returns Some(userId) if the user is part of the session or None if not
   */
  def checkJoinToken(session: ThinkingSession, token: Long): Option[Long] = {
    checkJoinToken(session.id, token)
  }

  /**
   * returns Some(userId) if the user is part of the session or None if not
   */
  def checkJoinToken(sessionId: Long, token: Long): Option[Long] = {
    DB.withConnection { implicit connection =>
      val sql = SQL("""select user
          from participating 
          where token={token} 
          and thinking_session={session}""").on('token -> token, 'session -> sessionId)
      sql.apply().headOption match {
        case Some(h) => Some(h[Long]("user"))
        case None    => None
      }
    }
  }

  def checkUser(sessionOption: Option[ThinkingSession], userOption: Option[User]): Boolean = {
    sessionOption match {
      case Some(s) =>
        checkUser(s, userOption)
      case None => false;
    }
  }

  def checkUser(session: ThinkingSession, userOption: Option[User]): Boolean = {
    userOption match {
      case Some(u) =>
        checkUser(session, u)
      case None => false;
    }
  }

  def checkUser(session: ThinkingSession, user: User): Boolean = {
    checkUser(session.id, user.id)
  }

  def checkUser(sessionId: Long, userId: Long): Boolean = {
    DB.withConnection { implicit connection =>
      val test = SQL("""
          select count(*) as num
          from participating 
          where user={user} 
          and thinking_session={session}""")
        .on('user -> userId,
          'session -> sessionId)
        .as(get[Long]("num").single)
      test > 0
    }
  }
  def checkCreationDate(sessionId: Long, userId: Long): Date = {
    DB.withConnection { implicit connection =>
      SQL("""select time from participating where user={user} and thinking_session={session}""")
        .on('user -> userId,
          'session -> sessionId)
        .as((get[Date]("time").single))
    }

  }

  def allUsersReady(session: ThinkingSession): Boolean = allUsersReady(session.id)
  def allUsersReady(sessionId: Long): Boolean = (userCount(sessionId) - readyCount(sessionId)) == 0
  /**
   * Dummy Session for dev purposes
   */
  val dummyId: Long = 0
  val dummy: ThinkingSession = byId(dummyId).get

}
