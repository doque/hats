$(function() {

	$('#modal-button').click(function() {
		/* new user? */
		var isNewUser = (USER_NAME === "New User");
		/* force new user to enter name */
		if (isNewUser) {
			var name = $('#modal-username');
			if (name.val() === "") {
				name.parent().addClass('has-error');
			} else {
				jsRoutes.controllers.Users.saveName().ajax({
					data : {
						"name" : name.val()
					},
					success : function() {
						USER_NAME = name.val();
						name.parent().removeClass('has-error');
						$('#first-time').remove();
						$('#hatchange-modal').modal('hide');
					}
				});
			}
		} else {
			$('#hatchange-modal').modal('hide');
		}

	});

	// only show first hat in progress bar
	if (typeof HAT !== "undefined") {
		//$('circle:not(.' + HAT + ')').hide();
	}

	window.progressBar = new ProgressBar('#progressBar');

	// setup initial card setup
	if (typeof CARDS !== "undefined" && CARDS.length > 0) {
		$(CARDS).each(function() {
			addCard(this);
		});
	}

	$('.tokenfield').tokenfield();

	$(document).on('click', '.addbucket', function() {
		addBucket();
	});
	$(document).on('blur', '.bucketname', function() {
		renameBucket(this); // this = element
	});
	$(document).on('click', 'a.fancybox', function(e) {
		e.preventDefault();
		var options = {
			speed : "slow"
		}
		var href = $(this).attr("href");
		if (!!~href.indexOf("youtube.com/watch?v=")) {
			$(this).attr("href", href.replace("watch?v=", "v/"));
			options.type = "swf";
			//options.swf= {"wmode":'transparent','allowfullscreen':'true'}
		}
		console.log(options);
		$.fancybox.open(this, options);
	});
	$('#indicate-finish').click(function() {
		location.href = "/" + SESSION_ID + "/dashboard";
	});

});
// get websocket up and running

function instantiateSocket() {
	// connect to WAMPlay server
	//console.log("Connecting to WAMPlay server...");
	// successful setup
	ab.connect(WSURI, function(session) {
		// click handler for add card
		$("#btnAddCard").click(function() {
			var newCard = {
				"thinkingSession" : SESSION_ID,
				"hat" : HAT,
				"content" : $("#content").val(),
				"username" : USER_NAME,
				"userId" : USER_ID
			};
			CARDS.push(newCard);
			var addCardEvent = {
				"eventType" : "addCard",
				"eventData" : newCard
			}
			var message = JSON.stringify(addCardEvent);
			session.call(CALL_URI + "#addCard", message)
		});

		$('#indicate-ready').click(function() {
			var hatInfo = {
				"thinkingSession" : SESSION_ID,
				"hat" : HAT
			};
			var moveHatEvent = {
				"eventType" : "moveHat",
				"eventData" : hatInfo
			};
			var message = JSON.stringify(moveHatEvent);
			session.call(CALL_URI + "#moveHat", message);
		});
		// console.log("Connected to " + WSURI);
		// subscribe to add cards here, give a callback
		// ID needs to be string
		session.subscribe(SESSION_ID.toString(), onEvent);
		//console.log("Subscribed to session number " + SESSION_ID);
	},

	// WAMP session is gone
	function(code, reason) {
		if (confirm('an error occured, reload?'))
			location.reload();
		//console.log("Connection lost (" + reason + ")", true);
		// should probably reconnect here
	},
	// additional options
	{
		skipSubprotocolCheck : true,
		skipSubprotocolAnnounce : true
	}); // Important! Play rejects all subprotocols for some reason...
}

function setSessionData() {
    $('#session-info').html('Session time: ' + timeSince(CREATION_TIME));
    var date = $.datepicker.formatDate('dd/mm/yy', CREATION_TIME);
    $('#feed-creation-time').html('Session was created on <strong>' + date + '</strong>');
}

function timeSince(date) {

    var seconds = Math.floor((new Date() - date) / 1000);

    var interval = Math.floor(seconds / 31536000);

    interval = Math.floor(seconds / 3600);
    if (interval > 1) {
        return interval + " hours";
    }

    interval = Math.floor(seconds / 60);
    if (interval > 1) {
        return interval + " minutes";
    } else {
    	return "less than a minute"
    }
}

// debugging handler for websocket events coming in

function onEvent(topic, event) {

	// add switch case for topic here:

	//console.log("Message from topic: " + topic + ":");
	// event holds the actual message that is being sent
	//console.log(event);
	// event.username = "FooUser";
	// event.id = 1e4;
	//if (userid != incoming user) OR use skip paramters in session.send
	if (event.eventType === "addCard") {
		addCard(event.eventData, true);
	} else if (event.eventType === "moveHat") {
		moveTo(event.eventData.hat);
	}
	window.progressBar.add(event.eventData);
}

function moveTo(hat) {
	// CSS changes for mood
	$('#hat').removeClass().addClass(hat.toLowerCase());
	$('body').removeClass().addClass(hat.toLowerCase());
	$('#form-hat').val(hat.toLowerCase());

	// change tooltip text for input
	var modal = $('#hatchange-modal');
	$('.hat', modal).html(hat.toLowerCase());
	$('.message', modal).html(TOOLTIPS[hat.toLowerCase()]);

	modal.modal();

	// overwrite global HAT var
	HAT = hat.toLowerCase();
	console.log("changed to %s hat", HAT);
	location.hash = HAT;

	if (HAT === "blue") {
		prepareBlueHat();
	}

	setSessionData();

}

// bucket should be {id, name}

function addBucket() {
	// get bucket info from server
	jsRoutes.controllers.Cards.createBucket(SESSION_ID).ajax(
			{
				success : function(bucket) {
					console.log("adding a bucket", bucket);
					//console.log("todo: remove dummy bucket in addBucket()");
					var template = Handlebars.compile($('#bucket-template')
							.html());
					var compiled = template(bucket).toString(); // workaround   
					// workaround
					$('#buckets-list').append(compiled).find('div.bucket')
							.droppable(options.droppable); //.sortable(sortOptions);
				}
			});

}

function renameBucket(elem) {
	// post bucketname to server
	var name = $(elem).val();
	// ajax to bucket name change

	var bucketId = $(elem).data("bucketid");
	jsRoutes.controllers.Cards.renameBucket(bucketId).ajax({
		data : {
			"name" : name
		},
		// have to use complete since no data is returned
		complete : function(e) {
			console.log("renamed bucket");
			$(elem).parent().find("h4").removeClass("hide").text(name);
			$(elem).remove();
		}
	});
}

function prepareBlueHat() {

	console.log("preparing blue hat, administrative controls enabled");

	enableDragDrop();

	// toggle buttons
	$('#indicate-finish').removeClass('hide');
	$('#indicate-ready').addClass('hide');

	// enable buckets here
	$('#buckets').removeClass("hide");
	addBucket();
}

function addCard(card, effect) {

	/**
	 * card json: {"id":5, "hat": "Green", "content": "card content",
	 * "username":"username"}
	 */
	// create clickable links
	if (card.content.trim() === "")
		return;
	card.content = linkify(card.content);

	var template = Handlebars.compile($('#card-template').html());
	var compiled = template(card);

	$('#cards-list').append(compiled);

	//if (effect) markup.effect('highlight', {}, 1000);

	// reset card content field
	$('#content').val("");
	$('#nocardsyet').remove();

	window.progressBar.add(card);
	if (HAT === "blue") {
		enableDragDrop();
	}

}

function enableDragDrop() {
	$('#cards-list div.card').draggable(options.draggable);
}

// jquery ui options

var options = {
	// drag options for cards
	draggable : {
		containment : "#hat-cards",
		cursor : "move",
		stack : "div.card",
		// snap: true,
		revert : "invalid" // revert, if not dropped to droppable
	},

	droppable : {
		hoverClass : "dropit",
		drop : function(event, ui) {
			// grab bucket id
			var bucketId = $(event.target).data('bucketid');
			// kill placeholder
			$(this).find(".placeholder").remove();
			// bind card
			var card = ui.draggable, cardId = card.data('cardid');
			
			//console.log(bucketId, cardId);
			
			// css fix
			card.css("position", "").off(); // unbind all drag shit
			card.draggable("disable"); // disable further dragging
			// inject into container
			$(this).find(".cards").append(card);
			// finally, post
			jsRoutes.controllers.Cards.addCardToBucket(bucketId, cardId).ajax({
				method : "post"
			});
		}
	},
	fancybox : {
		speed : "slow"
	// override youtube,
	}
};

function linkify(text) {
	// only use direct jp(e)g/png links or youtube links
	var exp = /(\b(https?|ftp|file):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/ig;
	return text.replace(exp, "<a class='fancybox' href='$1'>$1</a>");
}
