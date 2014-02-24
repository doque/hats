/**
 *  ProgressBar class
 *  @author dbusser
 */

/**
 * Constructor
 *
 * @param container - a selector which holds the progress bar (should be type svg)
 * @bubbles (optional) - initial bubbles to be set up with custom counts
 */

 function ProgressBar(container, bubbles) {
 	this.container = container;
 	this.hats = [
 	"white",
 	"red",
 	"yellow",
 	"black",
 	"green",
 	"blue"
 	];
 	this.algorithmParams = {
 		maxBubbleSize: 20,
 		minBubbleSize: 5,
		stretch: 3, // bigger strecht -> need more cards to reach max size (scales linear)
		tippingPoint: 10
	}
	this.render();
	this.addTooltips(false);
}
/**
 * adds a card to the progress bar inside the corresponding hat
 *
 * @param card - a Card object that should be added to the progress bar
 */
 ProgressBar.prototype.add = function(card) {
	// console.log("adding card: ", card);   
	this.addTooltips(true);
	this.render();
	this.setBubbleSizes();
};

/**
 * refreshes the circles in the progress bar to hold corresponding sizes
 *
 */
 ProgressBar.prototype.render = function() {
 	var self = this;
 	var hats = self.hats;

 	for (var i = 0; i < hats.indexOf(HAT) + 1; i++) {
 		var cirque = $(self.container).find('circle.' + hats[i]);
 		var position = 84 - hats.indexOf(HAT) * 14 + i * 14;
 		var percentage = position + "%";
 		cirque.attr("cx", percentage);
 	}


 };

/**
 * Calculate bubble size using logistic distribution, see https://de.wikipedia.org/wiki/Logistische_Verteilung
 * to play with the parameters see plots at
 * http://fooplot.com/#W3sidHlwZSI6MCwiZXEiOiIyOC8oMStleHAoLXgvMykpLTgiLCJjb2xvciI6IiMwMDAwMDAifSx7InR5cGUiOjEwMDAsIndpbmRvdyI6WyItMjAuMDI2ODk0MzU5OTk5OTQiLCI0NC45NzMxMDU2Mzk5OTk4NiIsIi04LjExNjYxMTg1Mzg4Mjc5MSIsIjMxLjg4MzM4ODE0NjExNzEyNCJdLCJzaXplIjpbNjQ5LDM5OV19XQ--
 **/

 function bubbleSize(count, max, params) {
	/**
	 * stretch changes how many contributions are needed for full size
	 * stretch = 1 -> ~ 10 cards yield full size
	 * stretch = 2 -> ~ 20 cards yield full size
	 * etc
	 **/
	 var stretch = params.stretch;
	 var fullSize = params.maxBubbleSize;
	 var minSize = params.minBubbleSize;

	 var scaledCount = (max > params.tippingPoint) ? (params.tippingPoint * count / max) : count;
	 if (max > params.tippingPoint) {
	 	console.log("Max = " + max + " scaled original count " + count + " to scaled count " + scaledCount);
	 }
	 
	 var algoSize = Math.ceil( (fullSize + 8) / (1 + Math.exp( -( scaledCount / stretch ) ) ) -8);

	 return Math.max(algoSize, minSize);
	}


	ProgressBar.prototype.setBubbleSizes = function() {
		var hatCards = {
			"blue": 0,
			"white": 0,
			"red": 0,
			"yellow": 0,
			"green": 0,
			"black": 0
		};
		CARDS.forEach(function(card) {
			hatCards[card.hat]++;
		});

		var max = 0;
		for (var hat in hatCards) {
			if (hatCards[hat] > max) max = hatCards[hat];
		}

	console.log("max Cards is " + max);

	var params = window.progressBar.algorithmParams;
	for (var hat in hatCards) {
		var cirque = $(this.container).find('circle.' + hat);
		var size = bubbleSize(hatCards[hat], max, params);
		console.log("hat " + hat + " gets size " + size);
		cirque.attr("r", size);
	}
}

ProgressBar.prototype.addTooltips = function(update) {
	var hats = this.hats;
	for (var hat in hats) {
		var addedCards = 0;
		var users = [];
		CARDS.forEach(function(card) {
			if (card.hat === hats[hat]) {
				addedCards++;
				if (users.indexOf(card.username) === -1) {
					users.push(card.username);
				}
			}
		});

		var contributors = "";
		if (users.length != 0) {
			var userList = "<li>" + users.join("</li><li>") + "</li>";
			contributors = '<hr/>' + userList;
		}

		var color = hats[hat];

		if (!update) {
			$(this.container).find('circle.' + color).qtip({
				content: {
					text: users.length + ' users have added ' + addedCards + ' cards to the ' + color + ' hat so far. ' + contributors
				},
				style: {
					classes: 'qtip-bootstrap'
				}
			});
		} else {
			var qapi = $(this.container).find('circle.' + hats[hat]).data('qtip');
			var newContent = users.length + ' users have added ' + addedCards + ' cards to the ' + color + ' hat so far: ' + contributors
			//var newContent = 'No. of cards in ' + hats[hat] + ' hat: ' + addedCards + contributors;
			qapi.set('content.text', newContent);
		}
	}
}