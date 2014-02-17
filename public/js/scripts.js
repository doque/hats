function instantiateSocket() {
	ab.connect(WSURI, function(session) {
		// store session
		window.session = session;
		session.subscribe(SESSION_ID.toString(), onEvent);
	},

	// WAMP session is gone
	function(code, reason) {
		if (confirm('an error occured, reload?'))
			location.reload();
	},
	// additional options
	{
		skipSubprotocolCheck : true,
		skipSubprotocolAnnounce : true
	}); // Important! Play rejects all subprotocols for some reason...
}

function setSessionData() {
    $('#session-info').html('Session time: ' + timeSince(CREATION_TIME));
    var date = $.datepicker.formatDate('dd/mm/yy', new Date(CREATION_TIME));
    $('#feed-creation-time').html('Session was created on <strong>' + date + '</strong> by <strong>' + CREATOR_NAME) + '</strong>';
}

// debugging handler for websocket events coming in

function onEvent(topic, event) {

	//console.log("received event %s, data: ", event.eventType, event.eventData);

	// this is dirty.
    if (event.eventType === "addCard") {
        injectCard(event.eventData);
    } else if (event.eventType === "moveHat") {
        moveTo(event.eventData.hat);
    } else if (event.eventType === "addBucket") {
    	injectBucket(event.eventData);
    } else if (event.eventType === "renameBucket") {
    	renameBucket(event.eventData);
    } else if (event.eventType === "addCardToBucket") {
    	addCardToBucket(event.eventData);
    } else if (event.eventType === "userJoin") {
    	feedUserJoin(event.eventData);
    }
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
	window.eventData.hat = HAT;

	location.hash = HAT;

	if (HAT === "blue") {
		prepareBlueHat();
	}

	window.progressBar.render()

}

function injectBucket(bucket) {
	var template = Handlebars.compile($('#bucket-template').html());
	var compiled = template(bucket).toString(); // workaround   
	// workaround

	// if name

	$('#buckets-list').append(compiled).find('div.bucket').droppable(options.droppable);
}


function renameBucket(eventData) {
	var bucketId = ''+eventData.bucketId,
		bucketName = eventData.bucketName;
	var bucket = $('div#bucket-' + bucketId)
	bucket.find("h4").removeClass("hide").text(bucketName);
	bucket.find("input").remove();
}

function enableDragDrop() {
	$('#cards-list div.card').draggable(options.draggable);
}

function prepareBlueHat() {
	enableDragDrop();
	// toggle buttons
	$('#indicate-finish').removeClass('hide');
	$('#indicate-ready').addClass('hide');

	// enable buckets here
	$('#buckets').removeClass("hide"); 
}


function injectCard(card) {

	// insert a href etc to card
	card = linkify(card);

	var template = Handlebars.compile($('#card-template').html());
	var compiled = template(card);

	$('#cards-list').append(compiled);
	// reset card content field
	$('#content').val("");
	$('#nocardsyet').remove();

	window.progressBar.add(card);

	if (HAT === "blue") {
		// do this again for all cards (easier than grabbing just added card)
		enableDragDrop();
	}
}

function dropCard(event, ui) {
	// grab bucket id
	var bucketId = $(event.target).data('bucketid'),
		bucket = $(event.target);

	// bind card
	var card = ui.draggable, cardId = card.data('cardid');

	window.session.call(CALL_URI + "#addCardToBucket", $.extend({}, window.eventData, {
		cardId: cardId,
		bucketId: bucketId
	}));
}

function addCardToBucket(eventData) {

	var cardId = ''+eventData.cardId,
		bucketId = ''+eventData.bucketId;
	var card = $('#card-'+cardId), bucket = $('#bucket-'+bucketId);

	//console.log(card, bucket);

	// kill placeholder
	bucket.find(".placeholder").remove();

	// css fix
	card.css("position", "").off(); // unbind all drag shit
	card.draggable("disable"); // disable further dragging
	// inject into container
	bucket.find(".cards").append(card);
}


function feedUserJoin(user) {
	var userGlyph = '<span class="glyphicon glyphicon-user"></span>';
	$('#feed').prepend('<li>' + userGlyph + ' User <strong>' + user.username + '</strong> joined.</li>');
	$('#feed').children().first().effect('highlight',{}, 3000);
}

Handlebars.registerHelper('shortLink', function(str) {
	// strip protocol
	str =  str.split("//")[1];
	// we only want 12 chars of the link, plus "..." = 3 chars = 15 max.
	if (str.length > 15) {
		str = str.substr(0,12) + "...";
	}
    return new Handlebars.SafeString(str)
});
