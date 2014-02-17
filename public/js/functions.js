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

function formatTime(secs) {

	var sec_num = parseInt(secs, 10);
	var hours   = Math.floor(sec_num / 3600);
	var minutes = Math.floor((sec_num - (hours * 3600)) / 60);
	var seconds = sec_num - (hours * 3600) - (minutes * 60);

	var str = [];
	if (seconds > 0) str.push(seconds + "s");
	if (minutes > 0) str.push(minutes + "m");
	if (hours > 0) str.push(hours + "h");

	return str.reverse().join(" ");
}

function linkify(card) {

	// only use direct jp(e)g/png links or youtube links
	var re = /(\b(https?|ftp|file):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/ig;
	var matches = card.content.match(re);

	// no url found, just return card as it is
	if (matches === null) return card;

	var url = matches[0];
	// strip url from card
	card.content = card.content.replace(url, "<br/>");
	// if it's an image, append additional field
	if (/(png|jpe?g)$/.test(url.toLowerCase())) {
		card.imageUrl = url;
	} else {
		// no image link, just display text
		card.url = url;
	}

	return card;
}