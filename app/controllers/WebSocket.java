package controllers;

import java.io.IOException;
import java.util.Date;

import models.Bucket;
import models.Card;
import models.Event;
import models.EventType;
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
	static Option<Card> noCard = scala.Option.apply(null);

	public static String getTopicName(long id) {
		return "thinkingSession_" + id;
	}

	@onRPC("#addBucket")
	public static void addBucket(String sessionId, JsonNode[] args) {
		JsonNode eventData = args[0];
		long userId = eventData.get("userId").asLong();
		long thinkingSessionId = eventData.get("thinkingSessionId").asLong();
		long bucketId = Bucket.create(thinkingSessionId);

		Option<Bucket> bucket = Bucket.byId(bucketId);
		Option<User> user = User.byId(userId);

		Hat hat = Hat.byName("blue"); // this can only happen in the blue hat
										// anyway.
		ThinkingSession tSession = ThinkingSession.byId(thinkingSessionId)
				.get();

		if (user.isDefined() && bucket.isDefined()) {
			long eventId = Event.create(EventType.addCard(), tSession, hat,
					user, noCard, bucket, new Date());
			Option<Event> event = Event.byId(eventId);
			publishEvent(event.get(), thinkingSessionId);
		} else {
			sendError(thinkingSessionId, "bucket creation failed");
		}

	}

	@onRPC("#renameBucket")
	public static void renameBucket(String sessionId, JsonNode[] args) {
		JsonNode eventData = args[0];
		long bucketId = eventData.get("bucketId").asLong();
		long thinkingSessionId = eventData.get("thinkingSessionId").asLong();
		long userId = eventData.get("userId").asLong();
		Option<Bucket> bucket = Bucket.byId(bucketId);
		Option<User> user = User.byId(userId);
		ThinkingSession tSession = ThinkingSession.byId(thinkingSessionId)
				.get();

		Hat hat = Hat.byName("blue");

		if (bucket.isDefined()) {
			String name = eventData.get("name").asText();
			Bucket.saveName(name, bucketId);
			long eventId = Event.create(EventType.renameBucket(), tSession,
					hat, user, noCard, bucket, new Date());
			Option<Event> event = Event.byId(eventId);
			publishEvent(event.get(), thinkingSessionId);
		} else {
			sendError(0, "Bucket not defined");
		}
	}

	@onRPC("#addCardToBucket")
	public static void addCardToBucket(String sessionId, JsonNode[] args) {
		JsonNode eventData = args[0];
		long bucketId = eventData.get("bucketId").asLong();
		long thinkingSessionId = eventData.get("thinkingSessionId").asLong();
		long userId = eventData.get("userId").asLong();
		Option<Bucket> bucket = Bucket.byId(bucketId);
		Option<User> user = User.byId(userId);
		ThinkingSession tSession = ThinkingSession.byId(thinkingSessionId)
				.get();

		Hat hat = Hat.byName("blue");

		if (bucket.isDefined()) {
			String name = eventData.get("name").asText();
			Bucket.saveName(name, bucketId);
			long eventId = Event.create(EventType.addCardToBucket(), tSession,
					hat, user, noCard, noBucket, new Date());
			Option<Event> event = Event.byId(eventId);
			publishEvent(event.get(), thinkingSessionId);
		} else {
			sendError(0, "Bucket not defined");
		}
	}

	// call addCard server-side
	@onRPC("#addCard")
	public static void add(String sessionId, JsonNode[] args) {
		JsonNode eventData = args[0];

		// check if user exists
		long userId = eventData.get("userId").asLong();
		// hat color
		Hat hat = Hat.byName(eventData.get("hat").asText());

		Option<User> user = User.byId(userId);

		long thinkingSessionId = eventData.get("thinkingSessionId").asLong();

		if (User.byId(userId).isDefined()
				&& ThinkingSession.byId(thinkingSessionId).isDefined()) {
			ThinkingSession tSession = ThinkingSession.byId(thinkingSessionId)
					.get();
			String content = eventData.get("content").asText();
			long cardId = Card.create(content, tSession, hat, user.get());
			Option<Card> card = Card.byId(cardId);

			long eventId = Event.create(EventType.addCard(), tSession, hat,
					user, card, noBucket, new Date());
			Option<Event> event = Event.byId(eventId);

			publishEvent(event.get(), thinkingSessionId);
		} else {
			sendError(thinkingSessionId, "User not defined");
		}

	}

	public static void userJoined(String sessionId, JsonNode[] args)
			throws JsonProcessingException, IOException {
		JsonNode eventData = new ObjectMapper().readTree(args[0].asText()).get(
				"eventData");
		long thinkingSessionId = eventData.get("thinkingSession").asLong();
		Hat hat = Hat.byName(eventData.get("hat").asText());
		long userId = eventData.get("userId").asLong();
		Option<User> user = User.byId(userId);

		if (User.byId(userId).isDefined()
				&& ThinkingSession.byId(thinkingSessionId).isDefined()) {
			ThinkingSession tSession = ThinkingSession.byId(thinkingSessionId)
					.get();
			long eventId = Event.create(EventType.userJoin(), tSession, hat,
					user, noCard, noBucket, new Date());
			Option<Event> event = Event.byId(eventId);

			if (event.isDefined()) {
				publishEvent(event.get(), thinkingSessionId);
			} else {
				JsonNode error = Json.newObject();
				((ObjectNode) error).put("error", "500");
				WAMPlayServer.publish(String.valueOf(thinkingSessionId), error);
			}
		}

		// hat color
	}

	@onRPC("#moveHat")
	public static void moveHat(String sessionId, JsonNode[] args) {

		JsonNode eventData = args[0];

		long thinkingSessionId = eventData.get("thinkingSessionId").asLong();
		long nextHatId = HatFlow.nextDefaultHatId(ThinkingSession.byId(
				thinkingSessionId).get());
		ThinkingSession.changeHatTo(thinkingSessionId, nextHatId);

		long eventId = Event.create(EventType.moveHat(), thinkingSessionId,
				nextHatId, none, none, none, new Date());
		Option<Event> event = Event.byId(eventId);

		if (event.isDefined()) {
			publishEvent(event.get(), thinkingSessionId);
		} else {
			sendError(thinkingSessionId, "Event undefined");
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

	private static void sendError(long thinkingSessionId, String message) {
		JsonNode error = Json.newObject();
		((ObjectNode) error).put("message", message);
		((ObjectNode) error).put("error", "500");
		WAMPlayServer.publish(String.valueOf(thinkingSessionId), error);
	}

}