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
                var nameString = name.val()
                var dataString = 'new-name=' + nameString;
                $.ajax({
                    type: "POST",
                    dataType: "text/plain",
                    url: "/user/saveName",
                    data: dataString
                });
                USER_NAME = nameString;
                name.parent().removeClass('has-error');
                $('#first-time').remove();
                $('#hatchange-modal').modal('hide');
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

    // initialize tooltips
    $('.tooltipster').tooltipster();

    // setup initial card setup
    if (typeof CARDS !== "undefined" && CARDS.length > 0) {
        $(CARDS).each(function() {
            addCard(this);
        });
    }

    $('.tooltipster').tooltipster();
    $('.tokenfield').tokenfield();

    $(document).on('click', '.addbucket', function() {
        addBucket();
    });
    $(document).on('blur', '.bucketname', function() {
        renameBucket(this); // this = element
    })

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
                    "thinkingSession": SESSION_ID,
                    "hat": HAT,
                    "content": $("#content").val(),
                    "username": USER_NAME,
                    "userId": USER_ID
                };
                var addCardEvent = {
                    "eventType": "addCard",
                    "eventData": newCard
                }
                var message = JSON.stringify(addCardEvent);
                session.call(CALL_URI + "#addCard", message)
            });

            $('#indicate-ready').click(function() {
                var hatInfo = {
                    "thinkingSession": SESSION_ID,
                    "hat": HAT
                };
                var moveHatEvent = {
                    "eventType": "moveHat",
                    "eventData": hatInfo
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
        	if (confirm('an error occured, reload?')) location.reload();
            //console.log("Connection lost (" + reason + ")", true);
            // should probably reconnect here
        },
        // additional options
        {
            skipSubprotocolCheck: true,
            skipSubprotocolAnnounce: true
        }); // Important! Play rejects all subprotocols for some reason...
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

}


// bucket should be {id, name}

function addBucket() {
    // get bucket info from server
    jsRoutes.controllers.Cards.createBucket(SESSION_ID).ajax({
        success: function(bucket) {
        	console.log("adding a bucket", bucket);
            //console.log("todo: remove dummy bucket in addBucket()");
            var template = Handlebars.compile($('#bucket-template').html());
            var compiled = template(bucket).toString(); // workaround   
            // workaround
            $('#buckets-list').append(compiled).find('div.bucket').droppable(options.droppable); //.sortable(sortOptions);
        }
    });

}

function renameBucket(elem) {
    // post bucketname to server
    var name = $(elem).val();
    // ajax to bucket name change

    jsRoutes.controllers.Cards.renameBucket(SESSION_ID).ajax({
        data: {
            "name": name
        },
        // have to use complete since no data is returned
        complete: function(e) {
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
    draggable: {
        containment: "#hat-cards",
        cursor: "move",
        stack: "div.card",
        snap: true,
        revert: "invalid" // revert, if not dropped to droppable
    },

    droppable: {
    	hoverClass: "dropit",
        drop: function(event, ui) {
        	// grab bucket id
        	var bucketId = $(event.target).data('bucketid');
        	// kill placeholder
            $(this).find(".placeholder").remove();
            // bind card
            var card = ui.draggable, cardId = card.data('cardid');
            // css fix
            card.css("position", "").off(); // unbind all drag shit
            card.draggable("disable"); // disable further dragging
            // inject into container
            $(this).find(".cards").append(card);
            // finally, post
            jsRoutes.controllers.Cards.addCardToBucket(bucketId, cardId).ajax({method: "post"});
        }
    }

};