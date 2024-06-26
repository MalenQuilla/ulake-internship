$(document).ready(() => {
    initNavbar();
    routeReady();
});

function routeReady() {
	const ready = {
        "^/$": indexReady,
        "^/dataset": datasetReady,
        "^/patients": patientReady,
        "^/team": teamReady,
        "^/detect": detectReady,
        "^/classify": classifyReady,
		"^/cache": cacheReady
	};
	Object.keys(ready).forEach(r => {
	    if (new RegExp(r).test(window.location.pathname)) ready[r]();
	});
}

function initNavbar() {
	let currPos = window.location.pathname;
	$(`.nav-link[href="${currPos}"]`).addClass('active');
}

// index functions
function indexReady() {
}

// dataset functions
function datasetReady() {
}

// patients functions
function patientReady() {
	$.ajax({
		url: "/data/patients3cm",
		success: data => {
			const patients = $.csv.toObjects(data.replace(/^\s*[\r\n]/gm, ""));
			if (Array.isArray(patients) && patients.length > 0) {
				const t = document.querySelector("tbody");
				while (t.firstChild) t.removeChild(t.firstChild);
				patients.forEach(p => {
					t.appendChild($(tplPatientRow(p))[0]);
				});
			}
		}
	});
}

// detect functions
function detectReady() {
	const hrefs = window.location.href.split("/");
	const detectPos = hrefs.indexOf("detect");
	if (detectPos >= 0 && detectPos < hrefs.length - 1) {
		const id = hrefs[detectPos + 1];
		$.ajax({
			url: `/service/detect/${id}`,
			success: (data) => detectResultReady(data, id)
		});
	}
}

function detectResultReady(data, id) {
	if (!data.pre_results || !Array.isArray(data.pre_results)) return;
	const result = document.querySelector("#result");
	while (result.firstChild) result.removeChild(result.firstChild);
	let idx = 0;
	data.pre_results.forEach(candidate => {
		let c = {
		    idx: ++idx,
		    slice: candidate[0],
            confidence: candidate[1].toFixed(2),
            data: candidate[2],
        };
		result.appendChild($(tplDetectResultRow(c))[0]);
	});

	const summary = $("#summary");
	summary.removeClass("invisible");
    $.bindings({
		count: data.pre_results.length,
		name: id
    });
}

// classify functions
function classifyReady() {
	const hrefs = window.location.href.split("/");
	const detectPos = hrefs.indexOf("classify");
	if (detectPos >= 0 && detectPos < hrefs.length - 1) {
		const id = hrefs[detectPos + 1];
		setTimeout(() => {
			$.ajax({
				url: `/service/classify/${id}`,
				success: (data) => classifyResultReady(data, id)
			})}, 6000);
	}
}

function classifyResultReady(data, id) {
	if (!data.pre_results || !Array.isArray(data.pre_results)) return;
	const result = document.querySelector("#result");
	while (result.firstChild) result.removeChild(result.firstChild);
	let idx = 0;
	data.pre_results.forEach(candidate => {
		let c = {
		    idx: ++idx,
		    slice: candidate[0],
            confidence: candidate[1].toFixed(2),
            data: candidate[2],
        };
		result.appendChild($(tplClassifyResultRow(c))[0]);
	});

	const summary = $("#summary");
	summary.removeClass("invisible");
    $.bindings({
		count: data.pre_results.length,
		name: id
    });
}


// contact functions
function teamReady() {
}


let cacheId = [];
function cacheReady() {
	$.ajax({
		url: "/data/patients3cm",
		success: data => {
			const patients = $.csv.toObjects(data.replace(/^\s*[\r\n]/gm, ""));
			if (Array.isArray(patients) && patients.length > 0) {
				patients.forEach(p => cacheId.push(p.ID) );
				cacheId.slice(3);
			}
		}
	});
}

function cacheLastId() {
	if (cacheId.length > 0) {
		let id = cacheId.pop();
		console.log(`caching classify ${id}`);
		$.ajax({
			url: `/service/classify/${id}`,
			success: data => {
				console.log(`caching detect ${id}`);
				$.ajax({
					url: `/service/detect/${id}`,
					success: data => {
						cacheLastId();
					}
				})
			}
		})
	}
}
