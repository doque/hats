package controllers

import models._
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.nonEmptyText
import play.api._
import play.api.mvc.Controller
import play.api.mvc._
import play.api.mvc.Results
import play.api.mvc.Action
import play.api.mvc.Controller
import models.User

/**
 * Card Controller responsible for handling CRUD operations on ideas/cards (will be treated as synonyms,
 * as a card is merely the visual representation of an idea)
 *
 * @author: NemoOudeis
 */
object Users extends Controller {

  /**
   * Handles adding cards for a remote procedure call
   */
  def saveName = Action { implicit request =>
    val thinkingSessionId = request.body.asFormUrlEncoded match {
      case Some(map) =>
        map.get("thinkingSessionId") match {

          case Some(sessionIds) => sessionIds head
          case None => 0
        }
      case None => BadRequest
    }
    val name = request.body.asFormUrlEncoded match {
      case Some(map) =>
        map.get("name") match {

          case Some(names) => names head
          case None => "New User"
        }
      case None => "New User"
    }
    Logger.debug("Users.saveName " + name)
    request.cookies.get(User.idCookie) match {
      case Some(cookie) => // found user cookie
        User.byCookie(cookie) match {
          case Some(user) =>
            User.saveName(user, name);
            WebSocket.userJoined(user, java.lang.Long.parseLong(thinkingSessionId.toString));
            Ok
          case None => BadRequest
        }
      case None => BadRequest
    }
  }
}