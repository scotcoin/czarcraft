var NRS = (function(NRS, $, undefined) {
	NRS.forms.errorMessages.startForging = {
		"5": NRS.getLangString("ERROR_NOTFORGING_BALANCEORNEWACCOUNT")
	};

	NRS.forms.startForgingComplete = function(response, data) {
		if ("deadline" in response) {
			$("#forging_indicator").addClass("forging");
			$("#forging_indicator span").html("Forging");
			NRS.isForging = true;
			$.growl(NRS.getLangString("SUCCESS_FORGIN_STARTED"), {
				type: "success"
			});
		} else {
			NRS.isForging = false;
			$.growl(NRS.getLangString("ERROR_NOTFORGING_UNKNOWN_ERROR"), {
				type: 'danger'
			});
		}
	}

	NRS.forms.stopForgingComplete = function(response, data) {
		if ($("#stop_forging_modal .show_logout").css("display") == "inline") {
			NRS.logout();
			return;
		}

		$("#forging_indicator").removeClass("forging");
		$("#forging_indicator span").html("Not forging");

		NRS.isForging = false;

		if (response.foundAndStopped) {
			$.growl(NRS.getLangString("SUCCESS_FORGING_STOPPED"), {
				type: 'success'
			});
		} else {
			$.growl(NRS.getLangString("ERROR_NOTFORGING_BEGIN"), {
				type: 'danger'
			});
		}
	}

	$("#forging_indicator").click(function(e) {
		e.preventDefault();

		if (NRS.downloadingBlockchain) {
			$.growl(NRS.getLangString("ERROR_NOTFORGE_BLOCKCHAIN_DOWNLOAD"), {
				"type": "danger"
			});
		} else if (NRS.state.isScanning) {
			$.growl(NRS.getLangString("ERROR_NOTFORGE_BLOCKCHAIN_RESCAN"), {
				"type": "danger"
			});
		} else if (!NRS.accountInfo.publicKey) {
			$.growl(NRS.getLangString('ERROR_NOTFORGE_MISSING_PUBLICKEY'), {
				"type": "danger"
			});
		} else if (NRS.accountInfo.effectiveBalanceNXT == 0) {
			if (NRS.lastBlockHeight >= NRS.accountInfo.currentLeasingHeightFrom && NRS.lastBlockHeight <= NRS.accountInfo.currentLeasingHeightTo) {
				$.growl(NRS.getLangString("ERROR_NOTFORGE_BALANCE_LEASEDOUT"), {
					"type": "danger"
				});
			} else {
				$.growl(NRS.getLangString("ERROR_NOTFORGE_BALANCE_ZERO"), {
					"type": "danger"
				});
			}
		} else if ($(this).hasClass("forging")) {
			$("#stop_forging_modal").modal("show");
		} else {
			$("#start_forging_modal").modal("show");
		}
	});

	return NRS;
}(NRS || {}, jQuery));