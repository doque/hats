package controllers

import play.api._
import play.api.mvc.Controller
import play.api.mvc._
import play.api.mvc.Results
import controllers._
import views.html.defaultpages.notFound
import models._
import java.util.Date
import org.joda.time._
import scala.collection.mutable.Map

/**
 * Dashboard Controller responsible for showing the summary report
 *
 * @author: Anamika
 */
object Dashboard extends Controller {

  /**
   * show summary report
   */
  def showReport(id: Long) = Action { implicit request =>
    // val hatTime = new Map[String, Long]()
    val hatNameTime = Map[String, Long]() //mutable
    //hatTime: Map[String, Long] = Map()
    var hatName: String = "White"
    val test = request.cookies.get(User.idCookie)
    request.cookies.get(User.idCookie) match {
      case Some(cookie) => // found user cookie
        User.byCookie(cookie) match {
          case Some(user) => // user in db
            ThinkingSession.byId(id) match {
              case Some(session) => // session exists
                if (ThinkingSession.checkUser(session, user)) // check if user is part of session
                {
                  var crTime: Date = ThinkingSession.checkCreationDate(id, user.id)
                  val eventList: List[Event] = Event.byThinkingSession(id)
                  Logger.debug("Session creation time" + crTime)
                  var creatTime: DateTime = new DateTime(crTime)
                  for (sEvent <- eventList) {
                    val eTime: DateTime = new DateTime(sEvent.time)
                    val elapsedTime = (eTime.getMillis() - creatTime.getMillis()) / 1000
                    Logger.debug("Elapsed TIme::" + elapsedTime)
                    //adding elapsed time 
                    Logger.debug("Hat name:" + hatName)
                    hatNameTime += (hatName -> elapsedTime)
                    creatTime = eTime
                    hatName = sEvent.hat.name

                  }
                  var endTime = DateTime.now()
                  Logger.debug("Hat name:" + hatName)
                  val elapsedTime1 = (endTime.getMillis() - creatTime.getMillis()) / 1000
                  hatNameTime += ((hatName -> elapsedTime1))
                  val hatElapsedTime: List[(String, Long)] = hatNameTime.toList
                  Ok(views.html.dashboard(hatElapsedTime, Card.byOnlyInSession(id), byUserCardList(id)))
                } else
                  BadRequest
              case None =>
                NotFound
            }
          case None => BadRequest
        }
      case None => BadRequest;
    }

  }
  def byUserCardList(id: Long): List[(String, List[Long])] = {
    var usrIDs: List[Long] = Card.byOnlyInSession(id) //only the Users
    val resultVal = Map[String, List[Long]]() //mutable 
    val hCards = Map[String, Long]() //mutable 
    val hats: List[Hat] = Hat.all();
    for (sHat <- hats) {
      val hList: List[Long] = checkHatsforUser(id, sHat)
      resultVal += (sHat.name -> hList)
    }
    Logger.debug("User for Current Session::" + resultVal)
    resultVal.toList
  }

  def checkHatsforUser(id: Long, sHat: Hat): List[Long] = {
    var usrIDs: List[Long] = Card.byOnlyInSession(id) //only the Users
    var mUList = scala.collection.mutable.ListBuffer[Long]()
    for (sUsr <- usrIDs) {
      mUList += Card.byCardsforUser(id, sHat.id, sUsr)
    }
    mUList.toList
  }
}