(function (ui) {
	var closePopup = ui.closePopup.bind(ui);

	document.getElementById('listPopup-cancel').addEventListener('click', closePopup, false);
	
	document.querySelector('#listPopup .ui-listview').addEventListener('click', closePopup, false);
})(window.tau);
