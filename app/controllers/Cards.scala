package controllers

import models.Card
import models.Hat
import models.User
import models.forms.FormCard
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

/**
 * Card Controller responsible for handling CRUD operations on ideas/cards (will be treated as synonyms,
 * as a card is merely the visual representation of an idea)
 *
 * @author: NemoOudeis
 */
object Cards extends Controller {

  /**
   * Form to create a card
   */
  val cardForm = Form(
    mapping(
      "content" -> nonEmptyText,
      "hat" -> nonEmptyText)(FormCard.apply)(FormCard.unapply))

  /**
   * Needed Info from HTML Params or (browser)session/user info:
   * - HatID : Long  		Dummy for now
   * - UserID : Long		Dummy for now
   * - content : String
   * Content needs to be a string obviously.
   * To use this endpoint see CardForm (below)
   */
  def addCard(thinkingSessionId: Long) = Action { implicit request =>
    Logger.debug("Cards.addCard")
    val form = cardForm.bindFromRequest.get
    val cardId = Card.create(form, thinkingSessionId, User.dummyUser1Id)
    Redirect(routes.ThinkingSessions.index(thinkingSessionId))
  }

  /**
   * TODO: retrieve card specified by session id and card id from db and update id. Redirect to session index
   * Needed Info from HTML Params:
   * - newContent: String
   */
  def editCard(sessionId: Long, cardId: Long) = TODO

  /**
   * TODO: delete Card specified by the ids, redirect to session index
   */
  def deleteCard(id: Long, cardId: Long) = TODO

  def restFormAddCard(sessionId: Long) = Action { implicit request =>
    val formCard = cardForm.bindFromRequest.get
    val user = User.dummyUser1;
    val hat = Hat.getByName(formCard.hat);
    val cardId = Card.create(formCard.content, sessionId, hat.id, user.id);
    Ok(Json.obj("id" -> cardId,
      "hat" -> hat.name.toLowerCase,
      "content" -> formCard.content,
      "username" -> user.name)).as("application/json")
  }

  def restJsonAddCard(sessionId: Long, hatId: Long) = Action(parse.json) { implicit request =>
    Logger.debug("Cards.restAddCard")
    val formCard = cardForm.bindFromRequest.get
    (request.body) match {
      case body: JsObject =>
        body \ "name" match {
          case JsString(name) =>
            if (name == "content") {
              request.body \ "value" match {
                case JsString(content) =>
                  val user = User.dummyUser1;
                  val hat = Hat.getById(hatId)
                  val cardId = Card.create(content, sessionId, hatId, user.id)

                  val json = Json.obj(
                    "status" -> 200,
                    "fn" -> "createCard",
                    "args" -> Json.obj(
                      "id" -> cardId,
                      "hat" -> hat.name,
                      "content" -> content,
                      "user" -> user.name))
                  // Return reponse with 200 (OK) status and JSON body
                  Ok(Json.obj("content" -> json)).as("application/json")

                case _ => BadRequest(Json.obj("error" -> true,
                  "message" -> "Could not match value =(")).as("application/json")
              }
            } else {
              BadRequest(Json.obj("error" -> true,
                "message" -> "Name was not content =(")).as("application/json")
            }
          case _ =>
            BadRequest(Json.obj("error" -> true,
              "message" -> "Could not find name =(")).as("application/json")
        }
      case _ =>
        BadRequest(Json.obj("error" -> true,
          "message" -> "Request body not a JSON object =p")).as("application/json")

    }
  }

}