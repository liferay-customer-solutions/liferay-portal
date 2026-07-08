/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.util;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Karoline Silva
 */
public class JiraDocumentUtil {

	public static JSONObject createCodeBlock(String text) {
		return new JSONObject(
		).put(
			"content",
			new JSONArray(
			).put(
				new JSONObject(
				).put(
					"text", text
				).put(
					"type", "text"
				)
			)
		).put(
			"type", "codeBlock"
		);
	}

	public static JSONObject createHorizontalRule() {
		return new JSONObject(
		).put(
			"type", "rule"
		);
	}

	public static JSONObject createLinkParagraph(String text, String href) {
		JSONObject textJSONObject = new JSONObject(
		).put(
			"marks",
			new JSONArray(
			).put(
				new JSONObject(
				).put(
					"attrs",
					new JSONObject(
					).put(
						"href", href
					)
				).put(
					"type", "link"
				)
			)
		).put(
			"text", text
		).put(
			"type", "text"
		);

		return new JSONObject(
		).put(
			"content",
			new JSONArray(
			).put(
				textJSONObject
			)
		).put(
			"type", "paragraph"
		);
	}

	public static JSONObject createParagraph(String text, boolean bold) {
		JSONObject textJSONObject = new JSONObject(
		).put(
			"text", text
		).put(
			"type", "text"
		);

		if (bold) {
			textJSONObject.put(
				"marks",
				new JSONArray(
				).put(
					new JSONObject(
					).put(
						"type", "strong"
					)
				));
		}

		return new JSONObject(
		).put(
			"content",
			new JSONArray(
			).put(
				textJSONObject
			)
		).put(
			"type", "paragraph"
		);
	}

}