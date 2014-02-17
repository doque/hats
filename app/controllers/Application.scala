package controllers

import models._
import controllers.ThinkingSessions._

import play.api._
import play.api.mvc._
import play.core.Router.JavascriptReverseRoute

/**
 * Reponsible for all ThinkingSession setup and creation.
 * If we ever add user accounts this controller should handle them too
 */
object Application extends Controller with UserCookieHandler {

  /**
   * Landing Page
   */
  def index = Action { implicit request =>
    Logger.debug("Application.index")
    val user = cookieUser(request) match {
      case Some(u) => u
      case None    => User.byId(User.create("New User", None)).get
    }

    val cookie = Cookie(User.idCookie, user.id.toString, Some(Int.MaxValue))
    Ok(views.html.index("Six Thinking Hats", user, sessionConfigForm)).withCookies(cookie)
  }

  def javascriptRoutes = Action { implicit request =>
    Logger.debug("Application.jsRoutes")
    import play.api.Routes

    Ok(
      Routes.javascriptRouter("jsRoutes")(
        routes.javascript.ThinkingSessions.createSession,
        routes.javascript.Users.saveName,
        routes.javascript.ThinkingSessions.closeSession)).as("text/javascript")
  }

}