package controllers

import play.api.mvc.Request
import play.api.mvc.Cookie
import models.User
import play.api.mvc.AnyContent

trait UserCookieHandler {

  def cookieUser(request: Request[AnyContent]): Option[User] = {
    request.cookies.get(User.idCookie) match {
      case Some(cookie) =>
        User.byCookie(cookie)
      case None => None
    }
  }
}