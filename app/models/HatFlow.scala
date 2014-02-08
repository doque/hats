package models

import play.api.db.DB
import play.api.db._

import play.api.Play.current
import anorm._
import anorm.SqlParser._

/**
 * Simple model obj for work flow storage (i.e. sequence of hats)
 * For timeLimit and aloneTime negative values (-1 will be default) indicates no limitation (e.g. no alone time)
 * @author Nemo
 */
case class HatFlow(index: Long, thinkingSession: Long, hat: Long)

object HatFlow {

  /**
   * ORM simple
   */
  val DBParser = {
    get[Long]("index") ~
      get[Long]("thinking_session") ~
      get[Long]("hat") map {
        case index ~ sessionId ~ hatId => HatFlow(index, sessionId, hatId);
      }
  }

  /**
   * Retrieve the HatFlow list for the specified session obj from db
   */
  def bySession(session: ThinkingSession): List[HatFlow] = {
    bySessionId(session.id)
  }

  /**
   * Retrieve the HatFlow list for the specified session from db
   */
  def bySessionId(sessionId: Long): List[HatFlow] = {
    DB.withConnection { implicit connection =>
      SQL("select * from hat_flow where thinking_session={sessionId}").on(
        'sessionId -> sessionId).as(HatFlow.DBParser *)
    }
  }

  /**
   * Get the next hat id for the input ThinkingSesssion obj
   */
  def nextHatId(session: ThinkingSession): Long = {
    nextHatId(session.id, session.currentHat.id)
  }

  /**
   * get the next hat of the session given a current hat
   */
  def nextHatId(sessionId: Long, currentHatId: Long): Long = {
    nextHatId(bySessionId(sessionId), currentHatId)
  }
  /**
   * given a list of HatFlow and the current hat this will return the id of the next Hat in the workflow
   */
  def nextHatId(flow: List[HatFlow], currentHatId: Long): Long = {
    val currentHat = (flow filter ((hatFlow: HatFlow) => hatFlow.hat == currentHatId)).head
    (flow filter ((hatFlow: HatFlow) => hatFlow.index == ((currentHat.index + 1) % (flow size)))).head.hat
  }

  /**
   * dummy hat flow for dev purposes. it goes like this:
   * White -> Green -> Yellow -> Red -> Black -> Blue
   */
  def defaultHatFlow: List[HatFlow] = bySessionId(ThinkingSession.dummyId)

  /**
   * for a given session the next hat id according to the dummy workflow
   */
  def nextDefaultHatId(session: ThinkingSession): Long = {
    nextHatId(defaultHatFlow, session.currentHat.id)
  }

}
