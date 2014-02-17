package models

import play.api.db.DB
import play.api.db._
import play.api.Logger
import play.api.mvc.Cookie
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import scala.util.Random

/**
 * Models participants and moderators/creators of ThinkingSession. No authentication done by us, so
 * we keep the data to a minimum.
 * Possible Additions: eMail, Image/ImageUrl, last (browser) session, list of ThinkingSessions (access)
 *
 * We do not need the Users for the this Monday (2nd Dec)
 *
 * @author Nemo
 */
case class User(id: Long, name: String, mail: Option[String])

object User {

  val idCookie = "hatUser";

  /**
   * ORM simple
   */
  def simple = {
    get[Long]("id") ~
      get[String]("name") ~
      (get[String]("mail") ?) map {
        case id ~ name ~ mail => User(id, name, mail)
      }
  }

  /**
   * Retrieve all users in the system
   */
  def all(): List[User] = {
    DB.withConnection { implicit connection =>
      SQL("select * from user").as(User.simple *)
    }
  }

  /**
   * Find a user by id
   */
  def byId(id: Long): Option[User] = {
    DB.withConnection { implicit connection =>
      SQL("select * from user where id = {id}").on(
        'id -> id).as(User.simple *) headOption
    }
  }

  /**
   * Create a new user by a name.
   * This will NOT return the created User!
   */
  def create(name: String, mail: Option[String]): Long = {
    val id: Long = (name + System.currentTimeMillis).hashCode + Random.nextLong
    DB.withConnection { implicit connection =>
      SQL("insert into user (id,name,mail) values ({id},{name},{mail})").on(
        'id -> id,
        'name -> name,
        'mail -> mail).executeUpdate()
    }
    id
  }

  /**
   * Delete a user
   */
  def delete(user: User) = {
    DB.withConnection { implicit connection =>
      SQL("delete from user where id = {id}").on(
        'id -> user.id).executeUpdate()
    }
  }

  def byCookie(cookie: Cookie): Option[User] = byId(java.lang.Long.parseLong(cookie.value))

  def byMail(mail: String): Option[User] = {
    DB.withConnection { implicit connection =>
      SQL("select * from user where mail = {mail}").on(
        'mail -> mail).as(User.simple *) headOption
    }
  }

  def saveMail(user: User, mail: String): Int = saveMail(user.id, mail)
  def saveMail(userId: Long, mail: String): Int = {
    DB.withConnection { implicit connection =>
      SQL("""
          update user
          set mail = {mail}
          where id = {userId}
          """).on(
        'mail -> mail,
        'userId -> userId).executeUpdate()
    }
  }

  def saveName(user: User, name: String): Int = saveName(user.id, name)

  def saveName(userId: Long, name: String): Int = {
    DB.withConnection { implicit connection =>
      SQL("""
          update user
          set name = {name}
          where id = {userId}
          """).on(
        'name -> name,
        'userId -> userId).executeUpdate()
    }
  }
  def getUserName(usrId: Long): String = {
    DB.withConnection { implicit connection =>
      SQL("""select name  from user where id = {usrId}""").on(
        'usrId -> usrId).as(get[String]("name").single)
    }
  }

  def bySession(id: Long): List[User] = {
    ThinkingSession.userIds(id).map(User.byId(_)).filter(_.isDefined).map(_.get)
  }

  /**
   * some dummy users for dev purposes
   */
  val dummyId = 1
  val dummyId2 = 2
  val dummyId3 = 3
  val dummyId4 = 4
  val dummy: User = all.head
  val dummy2: User = all.tail.head
  val dummy3: User = all.tail.tail.head
  val dummy4: User = all.tail.tail.tail.head
}