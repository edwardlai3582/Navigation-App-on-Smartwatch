(function() {
	var page = document.getElementById("hasSectionchangerPage"), 
		element = document.getElementById("sectionchanger"), 
		sectionChanger, 
		idx = 1;

	page.addEventListener("pageshow", function() {
		/* Create the SectionChanger object */
		sectionChanger = new tau.SectionChanger(element, {
			circular : true,
			orientation : "horizontal",
			useBouncingEffect : true
		});
	});

	page.addEventListener("pagehide", function() {
		/* Release the object */
		sectionChanger.destroy();
	});
})();