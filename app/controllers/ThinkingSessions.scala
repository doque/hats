package controllers

import play.api._
import play.api.mvc.Controller
import play.api.mvc._
import play.api.mvc.Results
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import models._
import controllers._
import com.feth.play.module.mail.Mailer
import com.feth.play.module.mail.Mailer.Mail.Body
import views.html.defaultpages.notFound
import ws.wamplay.controllers.WAMPlayServer
import scala.collection.JavaConversions

/**
 * Controls all changes in ThinkingSession state.
 * @author Nemo
 */
object ThinkingSessions extends Controller {

  /**
   * Show the index of the current session
   */
  def index(id: Long) = Action { implicit request =>
    Logger.debug("ThinkingSessions.index")
    request.cookies.get(User.idCookie) match {
      case Some(cookie) => // found user cookie
        User.byCookie(cookie) match {
          case Some(user) => // user in db
            ThinkingSession.byId(id) match {
              case Some(session) => // session exists
                if (ThinkingSession.checkUser(session, user)) // check if user is part of session
                  Ok(views.html.cards(session, Card.byThinkingSession(id), session.currentHat, user))
                else
                  BadRequest
              case None =>
                NotFound
            }
          case None => BadRequest
        }
      case None => BadRequest;
    }
  }

  def join(id: Long, token: String) = Action {
    val t: Long = java.lang.Long.parseLong(token, 16)
    ThinkingSession.checkJoinToken(id, t) match {
      case Some(userId) => // set token
        Logger.debug("User " + userId + " joined session " + id)
        Redirect(routes.ThinkingSessions.index(id)).withCookies(Cookie(User.idCookie, userId.toString))
      case None =>
        BadRequest
    }
  }

  /**
   * TODO: Conclude session and redirect to review page
   */
  def closeSession(id: Long) = TODO

  /**
   * Give a participant/user the opportunity to show she is ready to move on to the next hat.
   * Needed Form Params:
   * - UserID : Long
   *
   */
  def indicateReady(id: Long) = TODO

  /*
   * val to initiate session
   */
  val sessionConfigForm: Form[SessionConfig] = Form(
    mapping(
      "topic" -> nonEmptyText,
      "adminMailAddress" -> optional(text),
      "mailAddresses" -> text)(SessionConfig.apply)(SessionConfig.unapply))

  /*
   * Create new Session
   */
  def createSession() = Action { implicit request =>
    Logger.debug("ThinkingSessions.createSession")
    val form = sessionConfigForm.bindFromRequest.get;
    request.cookies.get(User.idCookie) match {
      case Some(cookie) =>
        User.byCookie(cookie) match {
          case Some(user) =>
            val newSessionId = ThinkingSession.create(user, form.topic, Hat.dummy)
            if (user.mail == None) {
              User.saveMail(user, form.adminMail.get)
            }
            val mailsAndTokens = addUsersToSessions(form.mailAddressList, newSessionId)
            sendInviteMails(mailsAndTokens, form.topic, newSessionId)
            ThinkingSession.addUser(newSessionId, user.id)

            WAMPlayServer.addTopic(newSessionId.toString)
            Logger.debug("Found user cookie, creating session " + newSessionId)
            Redirect(routes.ThinkingSessions.index(newSessionId))

          case None => BadRequest
        }
      case None =>
        Logger.debug("No user cookie, bad request")
        BadRequest
    }
  }

  def sendInviteMails(mails: List[(String, Long)], title: String, sessionId: scala.Long)(implicit request: Request[AnyContent]) {
    mails match {
      case (m, t) :: ms =>
        def toHexString(l: Long): String = if (l < 0l) "-" + (-1 * l).toHexString else l.toHexString

        val url = routes.ThinkingSessions.join(sessionId, toHexString(t)).absoluteURL(false)(request)
        val body = new Body(views.txt.email.invite.render(title, url).toString(),
          views.html.email.invite.render(title, url).toString());
        Mailer.getDefaultMailer().sendMail("Invite to Thinking Session", body, m);
        Logger.debug("Invited User " + m + " to thinking session " + title)
        sendInviteMails(ms, title, sessionId)
      case Nil =>
        Logger.info("All Users invited to session " + title)
    }
  }

  /**
   * returns a list of (mail,token)
   */
  def addUsersToSessions(mails: List[String], sessionId: Long): List[(String, Long)] = {
    mails match {
      case m :: ms =>
        val token: Long = User.byMail(m) match {
          case Some(u) => // user already exists
            Logger.debug("Adding existing user with mail " + m)
            ThinkingSession.addUser(sessionId, u.id)
          case None => // create new user
            Logger.debug("Adding new user with mail " + m)
            ThinkingSession.addUser(sessionId, User.create("New User", Some(m)))
        }
        (m, token) :: addUsersToSessions(ms, sessionId)
      case Nil => Nil
    }
  }
}