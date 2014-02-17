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
import play.api.libs.json.JsArray
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject

/**
 * Dashboard Controller responsible for showing the summary report
 *
 * @author: Anamika
 */
object Dashboard extends Controller with UserCookieHandler {

  /**
   * show summary report
   */
  def showReport(id: Long) = Action { implicit request =>
    val userOption = cookieUser(request)
    val sessionOption = ThinkingSession.byId(id)
    if (ThinkingSession.checkUser(sessionOption, userOption)) {

      // val hatTime = new Map[String, Long]()
      val hatNameTime = Map[String, Long]() //mutable
      //hatTime: Map[String, Long] = Map()
      var hatName: String = "White"
      var creTime: DateTime = new DateTime
      var creatTime: DateTime = new DateTime
      var eTime: DateTime = new DateTime
      val events: List[Event] = Event.byThinkingSessionId(id) // all event List for Current Session
      val hats: List[Hat] = Hat.all
      // This will measures the Elapsed time for each hats
      for (sHat <- hats) {
        for (sEvent <- events) {
          if ((sHat.id == sEvent.hat.id) && (sEvent.eventType == "createSession")) {
            var crTime: Date = sEvent.time;
            creatTime = new DateTime(crTime)
            creTime = creatTime
            //Logger.debug("CreateWhite::" + creTime)
          } else if ((sHat.id == sEvent.hat.id) && (sEvent.eventType == "moveHat")) {
            var crTime: Date = sEvent.time;
            eTime = new DateTime(crTime)
            //Logger.debug("ETIme::" + eTime)
            val elapsedTime = (eTime.getMillis() - creTime.getMillis()) / 1000
            hatNameTime += (hatName -> elapsedTime)
            creTime = eTime
          }
        }
        hatName = sHat.name
        //Logger.debug("Hatname::" + hatName)
      }
      var endTime = events.filter((e: Event) => e.eventType == EventType.closeSession).headOption match {
        case Some(event) => new DateTime(event.time)
        case None => DateTime.now()
      }
      //  xAxis: {
      //		categories: ['User 1', 'User 2', 'User 3', 'User 4']
      //	},
      val users: List[User] = User.bySession(id);

      val userJson = JsArray(
        users.map((u: User) => JsString(u.name)))

      def sortCardsByHat(hats: List[Hat], cards: List[Card]): List[(Hat, List[Card])] = {
        hats match {
          case h :: hs => (h, cards.filter(_.hat.name == h.name)) :: sortCardsByHat(hs, cards.filterNot(_.hat.name == h.name))
          case Nil => Nil
        }
      }

      def countUserCards(users: List[User], cards: List[Card]): List[Int] = {
        users match {
          case u :: us => cards.count(_.creator.id == u.id) :: countUserCards(us, cards.filterNot(_.creator.id == u.id))
          case Nil => Nil
        }
      }

      val cards = Card.byThinkingSession(id)
      val hatz: List[Hat] = HatFlow.defaultHatFlow.map((hf: HatFlow) => Hat.byId(hf.hat).get)
      val sortedCards: List[(Hat, List[Card])] = sortCardsByHat(hatz, cards)
      val seriesJson = JsArray(sortedCards.map(
        (x) => Json.obj(
          "name" -> x._1.name,
          "data" -> JsArray(countUserCards(users, x._2).map(JsNumber(_))),
          "colorKey" -> x._1.name.toLowerCase)))

      val elapsedTime1 = (endTime.getMillis() - creTime.getMillis()) / 1000
      hatNameTime += ((hatName -> elapsedTime1))
      val hatElapsedTime: List[(String, Long)] = hatNameTime.toList
      val bucketList: List[Bucket] = Bucket.byThinkingSessionId(id);
      Ok(views.html.dashboard(hatElapsedTime, Card.byThinkingSession(id), sessionOption.get.currentHat, bucketList, sessionOption.get,
        Json.stringify(userJson), Json.stringify(seriesJson), userOption.get))
    } else {
      Unauthorized
    }
  }

  /**
   * Return List of Hats which is a List of Users along with card numbers for the current session
   */
  def countHatsforUser(id: Long): List[(String, List[(String, Long)])] = {
    var usrIDs: List[Long] = Card.byOnlyInSession(id) //only the Users
    var uName: String = new String()
    //var mUList = scala.collection.mutable.ListBuffer[Long]()
    val uCards = Map[String, Long]() //mutable 
    val resultVal = Map[String, List[(String, Long)]]() //mutable 
    val hats: List[Hat] = Hat.all();
    for (sHat <- hats) {
      for (sUsr <- usrIDs) {
        uName = User.getUserName(sUsr)
        val cardNo: Long = Card.byCardsforUser(id, sHat.id, sUsr)
        uCards += (uName -> cardNo)
      }
      resultVal += (sHat.name -> uCards.toList)
    }
    Logger.debug("UserId With Cards: " + resultVal.toList)
    resultVal.toList
  }
}