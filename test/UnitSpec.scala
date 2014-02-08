import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import models._
import controllers._
import java.util.Date
@RunWith(classOf[JUnitRunner])
class UnitSpec extends Specification {

  "Models and Persistence Layer" should {
    "create and retrieve card" in new WithApplication {
      val content = "PersistenceSpecContent"
      val id = Card.create(content, ThinkingSession.dummyId, Hat.dummyId, User.dummyId)
      val dbCard = Card.byId(id)
      dbCard must beSome
      dbCard.get.id must equalTo(id)
      dbCard.get.content must equalTo(content)
      dbCard.get.thinkingSession.id must equalTo(ThinkingSession.dummyId)
      dbCard.get.hat.id must equalTo(Hat.dummyId)
      dbCard.get.creator.id must equalTo(User.dummyId)
    }

    "create and retrive ThinkingSession" in new WithApplication {
      val topic = "PersistenceSpecContent"
      val id = ThinkingSession.create(User.dummy, topic, Hat.dummy)
      val dbSession = ThinkingSession.byId(id);
      dbSession must beSome
      dbSession.get.id must equalTo(id)
      dbSession.get.title must equalTo(topic)
      dbSession.get.owner.id must equalTo(User.dummyId)
      dbSession.get.currentHat.id must equalTo(Hat.dummyId)
    }

    "create and retrive User" in new WithApplication {
      val name = "PersistenceSpecContent"
      val id = User.create(name, None);
      val dbUser = User.byId(id);
      dbUser.get.id must equalTo(id)
      dbUser must beSome
      dbUser.get.name must equalTo(name)
      dbUser.get.mail must equalTo(None)
    }

    "create and retrive Event" in new WithApplication {
      val name = "PersistenceSpecType"
      val date = new Date();
      val eventId = Event.create(name, ThinkingSession.dummyId, Hat.dummyId,
        None, None, None, date);
      val event = Event.byId(eventId)
      event must beSome
      event.get.eventType must beEqualTo(name)
      event.get.thinkingSession.id must beEqualTo(ThinkingSession.dummyId)
      event.get.hat.id must beEqualTo(Hat.dummyId)
      event.get.user must beNone
      event.get.card must beNone
      event.get.time.getTime() must beEqualTo(date.getTime())
    }

    "change hat of ThinkingSession" in new WithApplication {
      val topic = "PersistenceSpecContent2"
      val id = ThinkingSession.create(User.dummy, topic, Hat.dummy)
      val dbSession = ThinkingSession.byId(id);
      dbSession must beSome
      val hatId = HatFlow.nextDefaultHatId(dbSession.get)
      val newHat = Hat.byId(hatId);
      newHat must beSome
      ThinkingSession.changeHatTo(dbSession.get, newHat.get)
      val updatedDbSession = ThinkingSession.byId(id);
      updatedDbSession must beSome
      updatedDbSession.get.currentHat.id must beEqualTo(newHat.get.id)

    }
  }

  "Sending Mails" should {
    "test send mail" in new WithApplication {
      // add some emails to the list for testing
      val emails: List[(String, Long)] = Nil
      ThinkingSessions.sendInviteMails(emails, "Unit Test", ThinkingSession.dummyId)(FakeRequest())
    }
  }
}