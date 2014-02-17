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
		drop : function(e,ui) {
			return dropCard(e,ui);
		}
	},
	fancybox : {
		speed : "slow"
	}
};