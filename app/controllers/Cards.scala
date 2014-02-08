package controllers

import models._
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller
import java.util.Date
import ws.wamplay.controllers.WAMPlayServer

/**
 * Card Controller responsible for handling CRUD operations on ideas/cards (will be treated as synonyms,
 * as a card is merely the visual representation of an idea)
 *
 * @author: NemoOudeis
 */
object Cards extends Controller {

  def createBucket(id: Long) = Action { implicit request =>

    val user =
      request.cookies.get(User.idCookie) match {
        case Some(cookie) => User.byCookie(cookie)
        case None         => None
      }
    ThinkingSession.byId(id) match {
      case Some(session) =>
        if (ThinkingSession.checkUser(session, user)) {
          val bucketId = Bucket.create(session)
          Logger.debug("Creating Bucket for session " + id + " with bucket.id = " + bucketId)
          val bucket = Bucket.byId(bucketId)
          val eventId = Event.create("createBucket", session, session.currentHat, user, None, bucket, new Date())
          val event = Event.byId(eventId);
          WebSocket.publishEvent(event, id)
          Ok(bucket.get.asJson).as("application/json")
        } else BadRequest
      case None => NotFound
    }
  }

  def addCardToBucket(bucketId: Long, cardId: Long) = Action { implicit request =>
    val user = request.cookies.get(User.idCookie) match {
      case Some(cookie) => // found user cookie
        User.byCookie(cookie)
      case None => None
    }

    val bucket = Bucket.byId(bucketId)
    Bucket.addCard(bucketId, cardId);
    val session = ThinkingSession.byId(bucket.get.sessionId).get;
    val card = Card.byId(cardId)
    val eventId = Event.create("addCardToBucket", session, session.currentHat, user, card, bucket, new Date())
    val event = Event.byId(eventId)
    val topicName = controllers.WebSocket.getTopicName(session.id);
    WebSocket.publishEvent(event.get, session.id)
    Ok

  }

  // no return necessary
  def renameBucket(bucketId: Long) = Action { implicit request =>
    Logger.debug("Rename Bucket " + bucketId)
    val user = request.cookies.get(User.idCookie) match {
      case Some(cookie) => // found user cookie
        User.byCookie(cookie)
      case None => None
    }

    val name = request.body.asFormUrlEncoded match {
      case Some(map) => map.get("name")
      case None      => None
    }

    name match {
      case Some(n) =>
        Bucket.saveName(n.head, bucketId)
        val test = bucketId
        val bucket = Bucket.byId(bucketId);
        val session = ThinkingSession.byId(bucket.get.sessionId).get

        user match {
          case Some(u) =>
            val eventId = Event.create("renameBucket", session, session.currentHat, user, None, bucket, new Date())
            val event = Event.byId(eventId)
            val topicName = controllers.WebSocket.getTopicName(session.id);
            WebSocket.publishEvent(event.get, session.id)
            Ok
          case None => BadRequest
        }
      case None => BadRequest
    }
  }

}

