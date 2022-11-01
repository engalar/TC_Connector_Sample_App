// This file was generated by Mendix Studio Pro.
//
// WARNING: Only the following code will be retained when actions are regenerated:
// - the import list
// - the code between BEGIN USER CODE and END USER CODE
// - the code between BEGIN EXTRA CODE and END EXTRA CODE
// Other code you write will be lost the next time you deploy the project.
import { Big } from "big.js";

// BEGIN EXTRA CODE
function createObject(entityName) {
	const p = new Promise(function (resolve, reject) {
		mx.data.create({
			entity: entityName,
			callback: function (obj) {
				resolve(obj);
			},
			error: function (error) {
				reject(
					"Could not create object of entity " +
						entityName +
						": " +
						(error.message),
				);
			},
		});
	});
	return p;
}
// END EXTRA CODE

/**
 * @param {MxObject} modelDocument
 * @returns {Promise.<MxObject>}
 */
export async function __ParseBOMLineQueryInput(modelDocument) {
	// BEGIN USER CODE
	const modelId = modelDocument.get("Uid");
	const pos = modelId.indexOf("&psref&");
	if (pos <= 0) {
		return undefined;
	}
	const psref = modelId.substr(pos + 7);
	if (!psref) {
		return undefined;
	}
	const decodedPsref = decodeURIComponent(atob(psref).split("").map((c) => {
		return "%" + c.charCodeAt(0).toString(16);
	}).join(""));
	let query;
	try {
		query = JSON.parse(decodedPsref);
		if (!query) {
			return undefined;
		}
	} catch (e) {
		return undefined;
	}

	let itemRevision;
	if (query.itemRev?.uid) {
		itemRevision = await createObject("TcConnector.ItemRevision");
		itemRevision.set("UID", query.itemRev.uid);
	} else {
		return undefined;
	}

	let bomWindowPropFlagMap;
	if (query.bomWinPropFlagMap) {
		bomWindowPropFlagMap = await createObject(
			"TcConnector.BomWindowPropFlagMap",
		);
		bomWindowPropFlagMap.set(
			"show_unconfigured_variants",
			query.bomWinPropFlagMap.show_unconfigured_variants,
		);
		bomWindowPropFlagMap.set(
			"show_unconfigured_changes",
			query.bomWinPropFlagMap.show_unconfigured_changes,
		);
		bomWindowPropFlagMap.set(
			"show_suppressed_occurrences",
			query.bomWinPropFlagMap.show_suppressed_occurrences,
		);
		bomWindowPropFlagMap.set(
			"is_packed_by_default",
			query.bomWinPropFlagMap.is_packed_by_default,
		);
		bomWindowPropFlagMap.set(
			"show_out_of_context_lines",
			query.bomWinPropFlagMap.show_out_of_context_lines,
		);
		bomWindowPropFlagMap.set(
			"fnd0show_uncnf_occ_eff",
			query.bomWinPropFlagMap.fnd0show_uncnf_occ_eff,
		);
		bomWindowPropFlagMap.set(
			"fnd0bw_in_cv_cfg_to_load_md",
			query.bomWinPropFlagMap.fnd0bw_in_cv_cfg_to_load_md,
		);

		const bomWindowPropFlagMapUI = await createObject(
			"Viewer3D_TC.BomWindowPropFlagMapUI",
		);
		bomWindowPropFlagMapUI.addReference(
			"Viewer3D_TC.BomWindowPropFlagMapUI_BomWindowPropFlagMap",
			bomWindowPropFlagMap.getGuid(),
		);
		bomWindowPropFlagMapUI.set(
			"show_unconfigured_variants",
			query.bomWinPropFlagMap.show_unconfigured_variants === "true",
		);
		bomWindowPropFlagMapUI.set(
			"show_unconfigured_changes",
			query.bomWinPropFlagMap.show_unconfigured_changes === "true",
		);
		bomWindowPropFlagMapUI.set(
			"show_suppressed_occurrences",
			query.bomWinPropFlagMap.show_suppressed_occurrences === "true",
		);
		bomWindowPropFlagMapUI.set(
			"is_packed_by_default",
			query.bomWinPropFlagMap.is_packed_by_default === "true",
		);
		bomWindowPropFlagMapUI.set(
			"show_out_of_context_lines",
			query.bomWinPropFlagMap.show_out_of_context_lines === "true",
		);
		bomWindowPropFlagMapUI.set(
			"fnd0show_uncnf_occ_eff",
			query.bomWinPropFlagMap.fnd0show_uncnf_occ_eff === "true",
		);
		bomWindowPropFlagMapUI.set(
			"fnd0bw_in_cv_cfg_to_load_md",
			query.bomWinPropFlagMap.fnd0bw_in_cv_cfg_to_load_md === "true",
		);
	}

	let revisionRule;
	if (
		query.revRuleConfigInfo?.revRule?.uid &&
		query.revRuleConfigInfo.revRule.uid !== "AAAAAAAAAAAAAA"
	) {
		revisionRule = await createObject("TcConnector.RevisionRule");
		revisionRule.set("UID", query.revRuleConfigInfo.revRule.uid);
	}

	const createBomWindowInput = await createObject(
		"TcConnector.CreateBomWindowInput",
	);
	createBomWindowInput.addReference(
		"TcConnector.itemRev__BOMWindow",
		itemRevision.getGuid(),
	);
	if (revisionRule) {
		createBomWindowInput.addReference(
			"TcConnector.revRule__BOMWindow",
			revisionRule.getGuid(),
		);
	}
	if (bomWindowPropFlagMap) {
		createBomWindowInput.addReference(
			"TcConnector.bomWinPropFlagMap",
			bomWindowPropFlagMap.getGuid(),
		);
	}

	const getVariantRules = query.objectsForConfigure?.map(async (obj) => {
		if (obj.uid && obj.uid !== "AAAAAAAAAAAAAA") {
			const variantRule = await createObject("Viewer3D_TC.VariantRule");
			variantRule.set("UID", obj.uid);
			variantRule.set("isVariantRuleSelected", true);
			variantRule.addReference(
				"TcConnector.objectsForConfigure",
				createBomWindowInput.getGuid(),
			);
		}
	});
	if (getVariantRules?.length > 0) {
		await Promise.all(getVariantRules);
	}

	return createBomWindowInput;
	// END USER CODE
}
