package controllers;

import java.io.IOException;
import java.util.Date;

import models.Bucket;
import models.Card;
import models.Event;
import models.Hat;
import models.HatFlow;
import models.ThinkingSession;
import models.User;
import play.Logger;
import play.libs.Json;
import scala.Option;
import ws.wamplay.annotations.URIPrefix;
import ws.wamplay.annotations.onPublish;
import ws.wamplay.annotations.onRPC;
import ws.wamplay.annotations.onSubscribe;
import ws.wamplay.controllers.WAMPlayContoller;
import ws.wamplay.controllers.WAMPlayServer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@URIPrefix("http://sixhats.com/cards")
public class WebSocket extends WAMPlayContoller {
	// make scala option's None available in Java
	static Option<Object> none = scala.Option.apply(null);
	static Option<Bucket> noBucket = scala.Option.apply(null);

	public static String getTopicName(long id) {
		return "thinkingSession_" + id;
	}

	// call addCard server-side
	@onRPC("#addCard")
	public static void add(String sessionId, JsonNode[] args)
			throws JsonProcessingException, IOException {
		JsonNode eventData = new ObjectMapper().readTree(args[0].asText()).get(
				"eventData");
		// check if user exists
		long userId = eventData.get("userId").asLong();

		// hat color
		Hat hat = Hat.byName(eventData.get("hat").asText());
		Option<User> user = User.byId(userId);

		long thinkingSessionId = eventData.get("thinkingSession").asLong();

		if (User.byId(userId).isDefined()
				&& ThinkingSession.byId(thinkingSessionId).isDefined()) {;
			ThinkingSession tSession = ThinkingSession.byId(thinkingSessionId)
					.get();
			String content = eventData.get("content").asText();
			long cardId = Card.create(content, tSession, hat, user.get());
			Option<Card> card = Card.byId(cardId);

			
			long eventId = Event.create("addCard", tSession, hat, user, card,
					noBucket, new Date());
			Option<Event> event = Event.byId(eventId);
			
			publishEvent(event.get(), thinkingSessionId);
		} else {
			throw new IOException();
		}

	}

	@onRPC("#deleteCard")
	public static void deleteCard(String sessionId, JsonNode[] args) {
		// long eventId = Event.create("deleteCard", thinkingSessionId,
		// nextHatId,
		// none, none, new Date());
	}

	@onRPC("#moveHat")
	public static void moveHat(String sessionId, JsonNode[] args)
			throws JsonProcessingException, IOException {

		JsonNode data = new ObjectMapper().readTree(args[0].asText());

		long thinkingSessionId = data.get("eventData").get("thinkingSession")
				.asLong();
		long nextHatId = HatFlow.nextDefaultHatId(ThinkingSession.byId(
				thinkingSessionId).get());
		// final Hat nextHat = Hat.byId(nextHatId).get();

		ThinkingSession.changeHatTo(thinkingSessionId, nextHatId);

		long eventId = Event.create("moveHat", thinkingSessionId, nextHatId,
				none, none, none, new Date());
		Option<Event> event = Event.byId(eventId);

		if (event.isDefined()) {
			publishEvent(event.get(), thinkingSessionId);
		} else {
			JsonNode error = Json.newObject();
			((ObjectNode) error).put("error", "500");
			WAMPlayServer.publish(String.valueOf(thinkingSessionId), error);
		}
	}

	@onSubscribe
	public static boolean onSubscribe(String sessionID) {
		Logger.debug("subscribing session id" + sessionID);
		if (!WAMPlayServer.isTopic("thinkingSession_" + sessionID)) {
			WAMPlayServer.addTopic("thinkingSession_" + sessionID);
		}
		return true;
	}

	@onPublish
	public static JsonNode onPublish(String sessionID, JsonNode event) {

		return Json.toJson(event);
	}

	public static void publishEvent(Event event, long sessionId) {
		WAMPlayServer.publish(String.valueOf(sessionId), event.asJson());
	}

	public static void publishEvent(Option<Event> event, long sessionId) {
		if (event.isDefined()) {
			publishEvent(event.get(), sessionId);
		}
	}

}