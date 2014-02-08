package models

import models._

case class SessionConfig(
  topic: String,
  adminMailAddress: Option[String],
  mailAddresses: String) {

  def get(num: Option[Int]) = num match {
    case Some(x) => x
    case None    => -1
  }

  def mailAddressList: List[String] = mailAddresses.split(",").toList.filter(_ != "");
  def adminMail: Option[String] = adminMailAddress match {
    case Some(mail) =>
      Some(mail.split(",").toList.filter(_ != "").head)
    case None => None
  }

}