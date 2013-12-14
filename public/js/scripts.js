// change this variable depending on which hat is worn
var HAT = 'white';

$.fn.scrollView = function() {
	return this.each(function() {
		$('html, body').animate({
			scrollTop: $(this).offset().top - 10
		}, 1000);
	});
};

$(document).ready(function() {

	// initialize tooltips
	$('.tooltipster').tooltipster();

	// setup initial card setup
	if (CARDS.length > 0) {
		$.each(CARDS, function(card) {
			addCard(card); // no effect
		});
	}

	$('#card-form').ajaxForm({
		dataType: "json",
		type: "post",
		beforeSubmit: showRequest,
		success: function(data) {
			if (data.error === true) {
				alert(data.message);
				return;
			}
			addCard(data); // post-submit callback
		}
	});

});


function moveTo(hat) {
	// change hat class of div
	$('#hat').removeClass(HAT).addClass(hat.toLowerCase());
	// overwrite var
	HAT = hat;
}

function addCard(card, effect) {
	
	/**
	 * card json:
	 * {"id":5, "hat": "Green", "content": "card content", "user":"username"}
	 */

	var template = Handlebars.compile($('#card-template').html());
	var compiled = template(card);

	var markup = $(compiled).appendTo('#cards-list');
	if (effect) markup.effect('highlight', {}, 1000);
	
	// reset card content field
	$('#content').val('');
}


// from jQuery FromPlugin documentation:
// pre-submit callback

function showRequest(formData, jqForm, options) {
	// formData is an array; here we use $.param to convert it to a string to display it
	// but the form plugin does this for you automatically when it submits the data
	var queryString = $.param(formData);

	// jqForm is a jQuery object encapsulating the form element.  To access the
	// DOM element for the form do this:
	// var formElement = jqForm[0];

	alert('About to submit: \n\n' + queryString);

	// here we could return false to prevent the form from being submitted;
	// returning anything other than false will allow the form submit to continue
	return true;
}

// post-submit callback

function showResponse(responseText, statusText, xhr, $form) {
	// for normal html responses, the first argument to the success callback
	// is the XMLHttpRequest object's responseText property

	// if the ajaxForm method was passed an Options Object with the dataType
	// property set to 'xml' then the first argument to the success callback
	// is the XMLHttpRequest object's responseXML property

	// if the ajaxForm method was passed an Options Object with the dataType
	// property set to 'json' then the first argument to the success callback
	// is the json data object returned by the server

	alert('status: ' + statusText + '\n\nresponseText: \n' + responseText +
		'\n\nThe output div should have already been updated with the responseText.');
}