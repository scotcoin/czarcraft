var NRS = (function(NRS, $, undefined) {
	"use strict";

	NRS.server = "";
	NRS.state = {};
	NRS.blocks = [];
	NRS.genesis = "1763558929574856152";

	NRS.defaultUserLang = "en";
	NRS.userLang = NRS.defaultUserLang;
	NRS.langData = {};
	NRS.defaultLangData = {};
	
	NRS.account = "";
	NRS.accountRS = ""
	NRS.accountInfo = {};

	NRS.database = null;
	NRS.databaseSupport = false;

	NRS.settings = {};
	NRS.contacts = {};

	NRS.isTestNet = false;
	NRS.isLocalHost = false;
	NRS.isForging = false;
	NRS.isLeased = false;

	NRS.lastBlockHeight = 0;
	NRS.downloadingBlockchain = false;

	NRS.rememberPassword = false;
	NRS.selectedContext = null;

	NRS.currentPage = "dashboard";
	NRS.pages = {};
	NRS.incoming = {};

	NRS.hasLocalStorage = true;
	NRS.inApp = false;
	NRS.assetTableKeys = [];

	NRS.init = function() {
		//load language tokens

		NRS.userLang = navigator.language || navigator.userLanguage || NRS.defaultUserLang;
		NRS.userLang = NRS.userLang.split('-')[0];
		
		
		$.ajax({
			  dataType: "json",
			  url: "/js/lang/lang."+NRS.defaultUserLang+".json",
			  success: function(data){
				  NRS.defaultLangData = data;
				  if(NRS.userLang != NRS.defaultUserLang){
					  $.ajax({
						  dataType: "json",
						  url: "/js/lang/lang."+NRS.userLang+".json",
						  success: function(data){
							  NRS.langData = data;
							  $('[data-lang]').each(function(){
								  // texts have only to be changed for not en language
								  if (this.nodeName != 'INPUT'){
									  $(this).text(data[$(this).attr('data-lang')]);
								  }
								  else if(this.hasAttribute("value")){
									  $(this).val(data[$(this).attr('data-lang')]);
								  }
								  else if(this.hasAttribute("placeholder")){
									  $(this).attr('placeholder',data[$(this).attr('data-lang')]);
								  }
							  });
						  }
					});
				  } 
			  }
		});
		
		
		if (location.port && location.port != "9876") {
			$(".testnet_only").hide();
		} else {
			NRS.isTestNet = true;			
			$(".testnet_only, #testnet_login, #testnet_warning").show();
		}

		if (!NRS.server) {
			var hostName = window.location.hostname.toLowerCase();
			NRS.isLocalHost = hostName == "localhost" || hostName == "127.0.0.1" || NRS.isPrivateIP(hostName);
		}

		if (!NRS.isLocalHost) {
			$(".remote_warning").show();
		}

		try {
			window.localStorage;
		} catch (err) {
			NRS.hasLocalStorage = false;
		}

		NRS.createDatabase(function() {
			NRS.getSettings();
		});

		NRS.getState(function() {
			NRS.checkAliasVersions();
		});

		NRS.showLockscreen();

		if (window.parent && window.location.href.indexOf("?app") != -1) {
			NRS.inApp = true;

			$("#show_console").hide();

			parent.postMessage("loaded", "*");

			window.addEventListener("message", receiveMessage, false);
		}

		//every 30 seconds check for new block..
		setInterval(function() {
			NRS.getState();
		}, 1000 * 30);

		if (!NRS.isTestNet) {
			setInterval(NRS.checkAliasVersions, 1000 * 60 * 60);
		}

		NRS.allowLoginViaEnter();

		NRS.automaticallyCheckRecipient();

		$(".show_popover").popover({
			"trigger": "hover"
		});

		$("#dashboard_transactions_table, #transactions_table").on("mouseenter", "td.confirmations", function() {
			$(this).popover("show");
		}).on("mouseleave", "td.confirmations", function() {
			$(this).popover("destroy");
			$(".popover").remove();
		});

		$(window).on("resize.asset", function() {
			if (NRS.currentPage == "asset_exchange") {
				NRS.positionAssetSidebar();
			}
		});

		/*
		$("#asset_exchange_search input[name=q]").addClear({
			right: 0,
			top: 4,
			onClear: function(input) {
				$("#asset_exchange_search").trigger("submit");
			}
		});

		$("#id_search input[name=q], #alias_search input[name=q]").addClear({
			right: 0,
			top: 4
		});*/
	}

	// returns a string of a key in the current language (userLang)
	NRS.getLangString = function(key){
		return NRS.langData[key] || NRS.defaultLangData[key] || '---';
	};
	
	NRS.getState = function(callback) {
		NRS.sendRequest("getBlockchainStatus", function(response) {
			if (response.errorCode) {
				//todo
			} else {
				if (!("lastBlock" in NRS.state)) {
					//first time...
					NRS.state = response;

					$("#nrs_version").html(NRS.state.version).removeClass("loading_dots");

					NRS.getBlock(NRS.state.lastBlock, NRS.handleInitialBlocks);
				} else if (NRS.state.isScanning) {
					NRS.blocks = [];
					NRS.tempBlocks = [];
					NRS.getBlock(NRS.state.lastBlock, NRS.handleInitialBlocks);
					NRS.getInitialTransactions();
					if (NRS.account) {
						NRS.getAccountInfo();
					}
				} else if (NRS.state.lastBlock != response.lastBlock) {
					NRS.tempBlocks = [];
					NRS.state = response;
					if (NRS.account) {
						NRS.getAccountInfo();
					}
					NRS.getBlock(NRS.state.lastBlock, NRS.handleNewBlocks);
					if (NRS.account) {
						NRS.getNewTransactions();
					}
				} else {
					if (NRS.account) {
						NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
							NRS.handleIncomingTransactions(unconfirmedTransactions, false);
						});
					}
				}

				if (callback) {
					callback();
				}
			}
		});
	}

	$("#logo, .sidebar-menu a").click(function(event, data) {
		if ($(this).hasClass("ignore")) {
			$(this).removeClass("ignore");
			return;
		}

		event.preventDefault();

		if ($(this).data("toggle") == "modal") {
			return;
		}

		var page = $(this).data("page");

		if (page == NRS.currentPage) {
			if (data && data.callback) {
				data.callback();
			}
			return;
		}

		$(".page").hide();

		$(document.documentElement).scrollTop(0);

		$("#" + page + "_page").show();

		$(".content-header h1").find(".loading_dots").remove();

		var changeActive = !($(this).closest("ul").hasClass("treeview-menu"));

		if (changeActive) {
			var currentActive = $("ul.sidebar-menu > li.active");

			if (currentActive.hasClass("treeview")) {
				currentActive.children("a").first().addClass("ignore").click();
			} else {
				currentActive.removeClass("active");
			}

			if ($(this).attr("id") && $(this).attr("id") == "logo") {
				$("#dashboard_link").addClass("active");
			} else {
				$(this).parent().addClass("active");
			}
		}

		if (NRS.currentPage != "messages") {
			$("#inline_message_password").val("");
		}

		//NRS.previousPage = NRS.currentPage;
		NRS.currentPage = page;
		NRS.currentSubPage = "";

		if (NRS.pages[page]) {
			if (data && data.callback) {
				NRS.pages[page](data.callback);
			} else if (data) {
				NRS.pages[page](data);
			} else {
				NRS.pages[page]();
			}
		}
	});

	$("button.goto-page, a.goto-page").click(function(event) {
		event.preventDefault();

		NRS.goToPage($(this).data("page"));
	});

	NRS.goToPage = function(page) {
		var $link = $("ul.sidebar-menu a[data-page=" + page + "]");

		if ($link.length) {
			$link.trigger("click");
		} else {
			NRS.currentPage = page;
			$("ul.sidebar-menu a.active").removeClass("active");
			$(".page").hide();
			$("#" + page + "_page").show();
			if (NRS.pages[page]) {
				NRS.pages[page]();
			}
		}
	}

	NRS.pageLoading = function() {
		var $pageHeader = $("#" + NRS.currentPage + "_page .content-header h1");
		$pageHeader.find(".loading_dots").remove();
		$pageHeader.append("<span class='loading_dots'><span>.</span><span>.</span><span>.</span></span>");
	}

	NRS.pageLoaded = function(callback) {
		$("#" + NRS.currentPage + "_page .content-header h1").find(".loading_dots").remove();
		if (callback) {
			callback();
		}
	}

	NRS.createDatabase = function(callback) {
		var schema = {
			contacts: {
				id: {
					"primary": true,
					"autoincrement": true,
					"type": "NUMBER"
				},
				name: "VARCHAR(100) COLLATE NOCASE",
				email: "VARCHAR(200)",
				account: "VARCHAR(25)",
				accountRS: "VARCHAR(25)",
				description: "TEXT"
			},
			assets: {
				account: "VARCHAR(25)",
				accountRS: "VARCHAR(25)",
				asset: {
					"primary": true,
					"type": "VARCHAR(25)"
				},
				description: "TEXT",
				name: "VARCHAR(10)",
				decimals: "NUMBER",
				quantityQNT: "VARCHAR(15)",
				groupName: "VARCHAR(30) COLLATE NOCASE"
			},
			data: {
				id: {
					"primary": true,
					"type": "VARCHAR(40)"
				},
				contents: "TEXT"
			}
		};

		NRS.assetTableKeys = ["account", "accountRS", "asset", "description", "name", "position", "decimals", "quantityQNT", "groupName"];

		try {
			NRS.database = new WebDB("NRS_USER_DB", schema, 1, 4, function(error, db) {
				if (!error) {
					NRS.databaseSupport = true;

					NRS.loadContacts();

					NRS.database.select("data", [{
						"id": "asset_exchange_version"
					}], function(error, result) {
						if (!result.length) {
							NRS.database.delete("assets", [], function(error, affected) {
								if (!error) {
									NRS.database.insert("data", {
										"id": "asset_exchange_version",
										"contents": 2
									});
								}
							});
						}
					});

					NRS.database.select("data", [{
						"id": "closed_groups"
					}], function(error, result) {
						if (result.length) {
							NRS.closedGroups = result[0].contents.split("#");
						} else {
							NRS.database.insert("data", {
								id: "closed_groups",
								contents: ""
							});
						}
					});
					if (callback) {
						callback();
					}
				}
			});
		} catch (err) {
			NRS.database = null;
			NRS.databaseSupport = false;
		}
	}

	NRS.getAccountInfo = function(firstRun, callback) {
		NRS.sendRequest("getAccount", {
			"account": NRS.account
		}, function(response) {
			var previousAccountInfo = NRS.accountInfo;

			NRS.accountInfo = response;

			var preferredAccountFormat = (NRS.settings["reed_solomon"] ? NRS.accountRS : NRS.account);
			if (!preferredAccountFormat) {
				preferredAccountFormat = NRS.account;
			}
			if (response.errorCode) {
				$("#account_balance, #account_forged_balance").html("0");
				$("#account_nr_assets").html("0");

				if (NRS.accountInfo.errorCode == 5) {
					if (NRS.downloadingBlockchain) {
						$("#dashboard_message").addClass("alert-success").removeClass("alert-danger").html("The blockchain is currently downloading. Please wait until it is up to date." + (NRS.newlyCreatedAccount ? " Your account ID is: <strong>" + String(preferredAccountFormat).escapeHTML() + "</strong>" : "")).show();
					} else if (NRS.state && NRS.state.isScanning) {
						$("#dashboard_message").addClass("alert-danger").removeClass("alert-success").html("The blockchain is currently rescanning. Please wait until that has completed.").show();
					} else {
						$("#dashboard_message").addClass("alert-success").removeClass("alert-danger").html("Welcome to your brand new account. You should fund it with some coins. Your account ID is: <strong>" + String(preferredAccountFormat).escapeHTML() + "</strong>").show();
					}
				} else {
					$("#dashboard_message").addClass("alert-danger").removeClass("alert-success").html(NRS.accountInfo.errorDescription ? NRS.accountInfo.errorDescription.escapeHTML() : "An unknown error occured.").show();
				}
			} else {
				if (NRS.accountRS && NRS.accountInfo.accountRS != NRS.accountRS) {
					$.growl(NRS.getLangString("ERROR_ADDRESS_DIFFERENTFROMBLOCKCHAIN"), {
						"type": "danger"
					});
					NRS.accountRS = NRS.accountInfo.accountRS;
				}

				if (NRS.downloadingBlockchain) {
					$("#dashboard_message").addClass("alert-success").removeClass("alert-danger").html("The blockchain is currently downloading. Please wait until it is up to date." + (NRS.newlyCreatedAccount ? " Your account ID is: <strong>" + String(preferredAccountFormat).escapeHTML() + "</strong>" : "")).show();
				} else if (NRS.state && NRS.state.isScanning) {
					$("#dashboard_message").addClass("alert-danger").removeClass("alert-success").html("The blockchain is currently rescanning. Please wait until that has completed.").show();
				} else if (!NRS.accountInfo.publicKey) {
					$("#dashboard_message").addClass("alert-danger").removeClass("alert-success").html(NRS.getLangString("STRING_WARNING")+"!</b>: "+NRS.getLangString("DASHBOARD_PUBKEY_WARNING")+" (<a href='#' data-toggle='modal' data-target='#send_message_modal'>"+NRS.getLangString("SEND_A_MESSAGE")+"</a>, <a href='#' data-toggle='modal' data-target='#register_alias_modal'>"+NRS.getLangString("BUY_AN_ALIAS")+"</a>, <a href='#' data-toggle='modal' data-target='#send_money_modal'>"+NRS.getLangString("SEND_NFD")+"</a>, ...)").show();					
				} else {
					$("#dashboard_message").hide();
				}

				//only show if happened within last week
				var showAssetDifference = (!NRS.downloadingBlockchain || (NRS.blocks && NRS.blocks[0] && NRS.state && NRS.state.time - NRS.blocks[0].timestamp < 60 * 60 * 24 * 7));

				if (NRS.databaseSupport) {
					NRS.database.select("data", [{
						"id": "asset_balances_" + NRS.account
					}], function(error, asset_balance) {
						if (!error && asset_balance.length) {
							var previous_balances = asset_balance[0].contents;

							if (!NRS.accountInfo.assetBalances) {
								NRS.accountInfo.assetBalances = [];
							}

							var current_balances = JSON.stringify(NRS.accountInfo.assetBalances);

							if (previous_balances != current_balances) {
								if (previous_balances != "undefined" && typeof previous_balances != "undefined") {
									previous_balances = JSON.parse(previous_balances);
								} else {
									previous_balances = [];
								}
								NRS.database.update("data", {
									contents: current_balances
								}, [{
									id: "asset_balances_" + NRS.account
								}]);
								if (showAssetDifference) {
									NRS.checkAssetDifferences(NRS.accountInfo.assetBalances, previous_balances);
								}
							}
						} else {
							NRS.database.insert("data", {
								id: "asset_balances_" + NRS.account,
								contents: JSON.stringify(NRS.accountInfo.assetBalances)
							});
						}
					});
				} else if (showAssetDifference && previousAccountInfo && previousAccountInfo.assetBalances) {
					var previousBalances = JSON.stringify(previousAccountInfo.assetBalances);
					var currentBalances = JSON.stringify(NRS.accountInfo.assetBalances);

					if (previousBalances != currentBalances) {
						NRS.checkAssetDifferences(NRS.accountInfo.assetBalances, previousAccountInfo.assetBalances);
					}
				}

				$("#account_balance").html(NRS.formatStyledAmount(response.unconfirmedBalanceNQT));
				$("#account_forged_balance").html(NRS.formatStyledAmount(response.forgedBalanceNQT));

				var nr_assets = 0;

				if (response.assetBalances) {
					for (var i = 0; i < response.assetBalances.length; i++) {
						if (response.assetBalances[i].balanceQNT != "0") {
							nr_assets++;
						}
					}
				}

				$("#account_nr_assets").html(nr_assets);

				if (NRS.lastBlockHeight) {
					var isLeased = NRS.lastBlockHeight >= NRS.accountInfo.currentLeasingHeightFrom;
					if (isLeased != NRS.IsLeased) {
						var leasingChange = true;
						NRS.isLeased = isLeased;
					}
				} else {
					var leasingChange = false;
				}

				if (leasingChange ||
					(response.currentLeasingHeightFrom != previousAccountInfo.currentLeasingHeightFrom) ||
					(response.lessors && !previousAccountInfo.lessors) ||
					(!response.lessors && previousAccountInfo.lessors) ||
					(response.lessors && previousAccountInfo.lessors && response.lessors.sort().toString() != previousAccountInfo.lessors.sort().toString())) {
					NRS.updateAccountLeasingStatus();
				}

				if (response.name) {
					$("#account_name").html(response.name.escapeHTML());
				}
			}

			if (firstRun) {
				$("#account_balance, #account_forged_balance, #account_nr_assets").removeClass("loading_dots");
			}

			if (callback) {
				callback();
			}
		});
	}

	NRS.updateAccountLeasingStatus = function() {
		var accountLeasingLabel = "";
		var accountLeasingStatus = "";

		if (NRS.lastBlockHeight >= NRS.accountInfo.currentLeasingHeightFrom) {
			accountLeasingLabel = "Leased Out";
			accountLeasingStatus = "Your account effective balance is leased out starting from block " + String(NRS.accountInfo.currentLeasingHeightFrom).escapeHTML() + " until block " + String(NRS.accountInfo.currentLeasingHeightTo).escapeHTML() + " to account <a href='#' data-user='" + String(NRS.accountInfo.currentLessee).escapeHTML() + "' class='user_info'>" + String(NRS.accountInfo.currentLessee).escapeHTML() + "</a>";
			$("#lease_balance_message").html("<strong>Remember</strong>: This lease will take effect after the current lease has ended.");

		} else if (NRS.lastBlockHeight < NRS.accountInfo.currentLeasingHeightTo) {
			accountLeasingLabel = "Leased Soon";
			accountLeasingStatus = "Your account effective balance will be leased out starting from block " + String(NRS.accountInfo.currentLeasingHeightFrom).escapeHTML() + " until block " + String(NRS.accountInfo.currentLeasingHeightTo).escapeHTML() + " to account <a href='#' data-user='" + String(NRS.accountInfo.currentLessee).escapeHTML() + "' class='user_info'>" + String(NRS.accountInfo.currentLessee).escapeHTML() + "</a>";
			$("#lease_balance_message").html("<strong>Remember</strong>: This lease will take effect after the current lease has ended.");
		} else {
			accountLeasingStatus = "Your account effective balance is not leased out.";
			$("#lease_balance_message").html("<strong>Remember</strong>: Once submitted the lease cannot be cancelled.");
		}

		if (NRS.accountInfo.effectiveBalanceNXT == 0) {
			$("#forging_indicator").removeClass("forging");
			$("#forging_indicator span").html("Not Forging");
			$("#forging_indicator").show();
			NRS.isForging = false;
		}

		//no reed solomon available? do it myself? todo
		if (NRS.accountInfo.lessors) {
			if (accountLeasingLabel) {
				accountLeasingLabel += ", ";
				accountLeasingStatus += "<br /><br />";
			}
			accountLeasingLabel += NRS.accountInfo.lessors.length + (NRS.accountInfo.lessors.length == 1 ? " lessor" : "lessors");
			accountLeasingStatus += NRS.accountInfo.lessors.length + " " + (NRS.accountInfo.lessors.length == 1 ? "lessor has" : "lessors have") + " leased their effective balance to your account.";

			var rows = "";

			for (var i = 0; i < NRS.accountInfo.lessors.length; i++) {
				var lessor = NRS.accountInfo.lessors[i];

				rows += "<tr><td><a href='#' data-user='" + String(lessor).escapeHTML() + "'>" + NRS.getAccountTitle(lessor) + "</a></td></tr>";
			}

			$("#account_lessor_table tbody").empty().append(rows);
			$("#account_lessor_container").show();
		} else {
			$("#account_lessor_table tbody").empty();
			$("#account_lessor_container").hide();
		}

		if (accountLeasingLabel) {
			$("#account_leasing").html(accountLeasingLabel).show();
		} else {
			$("#account_leasing").hide();
		}

		if (accountLeasingStatus) {
			$("#account_leasing_status").html(accountLeasingStatus).show();
		} else {
			$("#account_leasing_status").hide();
		}
	}

	NRS.checkAssetDifferences = function(current_balances, previous_balances) {
		var current_balances_ = {};
		var previous_balances_ = {};

		if (previous_balances.length) {
			for (var k in previous_balances) {
				previous_balances_[previous_balances[k].asset] = previous_balances[k].balanceQNT;
			}
		}

		if (current_balances.length) {
			for (var k in current_balances) {
				current_balances_[current_balances[k].asset] = current_balances[k].balanceQNT;
			}
		}

		var diff = {};

		for (var k in previous_balances_) {
			if (!(k in current_balances_)) {
				diff[k] = "-" + previous_balances_[k];
			} else if (previous_balances_[k] !== current_balances_[k]) {
				var change = (new BigInteger(current_balances_[k]).subtract(new BigInteger(previous_balances_[k]))).toString();
				diff[k] = change;
			}
		}

		for (k in current_balances_) {
			if (!(k in previous_balances_)) {
				diff[k] = current_balances_[k]; // property is new
			}
		}

		var nr = Object.keys(diff).length;

		if (nr == 0) {
			return;
		} else if (nr <= 3) {
			for (k in diff) {
				NRS.sendRequest("getAsset", {
					"asset": k,
					"_extra": {
						"asset": k,
						"difference": diff[k]
					}
				}, function(asset, input) {
					if (asset.errorCode) {
						return;
					}
					asset.difference = input["_extra"].difference;
					asset.asset = input["_extra"].asset;

					var quantity = NRS.formatQuantity(asset.difference, asset.decimals);
					var assetLink = "<a href='#' data-goto-asset='" + String(asset.asset).escapeHTML() + "'>" + quantity + " " + String(asset.name).escapeHTML() + " asset(s)</a>";
					
					if (asset.difference.charAt(0) != "-") {

						$.growl(NRS.getLangString("MESSAGE_YOURECEIVED").replace(/\$1/g,assetLink), {
							"type": "success"
						});
					} else {
						asset.difference = asset.difference.substring(1);

						$.growl(NRS.getLangString("MESSAGE_YOUTRANSFERRED").replace(/\$1/g,assetLink), {
							"type": "success"
						});
					}
				});
			}
		} else {
			$.growl(NRS.getLangString("SUCCESS_MULTIPLE_ASSETS_SOLD"), {
				"type": "success"
			});
		}
	}

	NRS.checkLocationHash = function(password) {
		if (window.location.hash) {
			var hash = window.location.hash.replace("#", "").split(":")

			if (hash.length == 2) {
				if (hash[0] == "message") {
					var $modal = $("#send_message_modal");
				} else if (hash[0] == "send") {
					var $modal = $("#send_money_modal");
				} else if (hash[0] == "asset") {
					NRS.goToAsset(hash[1]);
					return;
				} else {
					var $modal = "";
				}

				if ($modal) {
					var account_id = String($.trim(hash[1]));
					if (!/^\d+$/.test(account_id) && account_id.indexOf("@") !== 0) {
						account_id = "@" + account_id;
					}

					$modal.find("input[name=recipient]").val(account_id.unescapeHTML()).trigger("blur");
					if (password && typeof password == "string") {
						$modal.find("input[name=secretPhrase]").val(password);
					}
					$modal.modal("show");
				}
			}

			window.location.hash = "#";
		}
	}

	NRS.updateBlockchainDownloadProgress = function() {
		if (NRS.state.lastBlockchainFeederHeight && NRS.state.numberOfBlocks < NRS.state.lastBlockchainFeederHeight) {
			var percentage = parseInt(Math.round((NRS.state.numberOfBlocks / NRS.state.lastBlockchainFeederHeight) * 100), 10);
		} else {
			var percentage = 100;
		}

		if (percentage == 100) {
			$("#downloading_blockchain .progress").hide();
		} else {
			$("#downloading_blockchain .progress").show();
			$("#downloading_blockchain .progress-bar").css("width", percentage + "%").prop("aria-valuenow", percentage);
			$("#downloading_blockchain .sr-only").html(percentage + "% Complete");
		}
	}

	$("#id_search").on("submit", function(e) {
		e.preventDefault();

		var id = $.trim($("#id_search input[name=q]").val());

		if (/NFD\-/i.test(id)) {
			NRS.sendRequest("getAccount", {
				"account": id
			}, function(response, input) {
				if (!response.errorCode) {
					response.account = input.account;
					NRS.showAccountModal(response);
				} else {
					$.growl(NRS.getLangString("ERROR_NOTHING_FOUND"), {
						"type": "danger"
					});
				}
			});
		} else {
			if (!/^\d+$/.test(id)) {
				$.growl(NRS.getLangString("ERROR_INVALID_INPUT_SOLOMON"), {
					"type": "danger"
				});
				return;
			}
			NRS.sendRequest("getTransaction", {
				"transaction": id
			}, function(response, input) {
				if (!response.errorCode) {
					response.transaction = input.transaction;
					NRS.showTransactionModal(response);
				} else {
					NRS.sendRequest("getAccount", {
						"account": id
					}, function(response, input) {
						if (!response.errorCode) {
							response.account = input.account;
							NRS.showAccountModal(response);
						} else {
							NRS.sendRequest("getBlock", {
								"block": id
							}, function(response, input) {
								if (!response.errorCode) {
									response.block = input.block;
									NRS.showBlockModal(response);
								} else {
									$.growl(NRS.getLangString("ERROR_NOTHING_FOUND"), {
										"type": "danger"
									});
								}
							});
						}
					});
				}
			});
		}
	});

	return NRS;
}(NRS || {}, jQuery));

$(document).ready(function() {
	NRS.init();
});

function receiveMessage(event) {
	if (event.origin != "file://") {
		return;
	}
	//parent.postMessage("from iframe", "file://");
}