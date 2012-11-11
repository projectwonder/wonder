package er.ajax.mootools;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOElement;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

import er.ajax.AjaxComponent;
import er.ajax.AjaxOption;
import er.ajax.AjaxOptions;
import er.ajax.AjaxUtils;
import er.extensions.foundation.ERXStringUtilities;
import er.extensions.foundation.ERXValueUtilities;

public class MTAjaxAutoComplete extends AjaxComponent {

	public String divName;
	public String fieldName;
	public String indicatorName;

	public MTAjaxAutoComplete(WOContext context) {
		super(context);
	}

	@Override
	protected void addRequiredWebResources(WOResponse res) {
		// TODO Auto-generated method stub
		MTAjaxUtils.addScriptResourceInHead(context(), res, "MooTools", MTAjaxUtils.MOOTOOLS_CORE_JS);
		MTAjaxUtils.addScriptResourceInHead(context(), res, "MooTools", "scripts/plugins/autocomplete/AutoCompleter.js");
	}

	/**
	 * Overridden to set the IDs for the field and the div tag.
	 */
	public void awake() {
		super.awake();
		divName = safeElementID() + "_div";
		fieldName = safeElementID() + "_field";
		indicatorName = safeElementID() + "_indicator";
	}

	public void sleep() {
		divName = null;
		fieldName = null;
		indicatorName = null;
		super.sleep();
	}	

	/**
	 * Overridden because the component is stateless
	 */
	public boolean isStateless() {
		return true;
	}

	/**
	 * Overridden because the component does not synch with the bindings.
	 */
	public boolean synchronizesVariablesWithBindings() {
		return false;
	}

	public String indicator() {
		String indicator = (String)valueForBinding("indicator");
		if (indicator == null && valueForBinding("indicatorFilename") != null) {
			indicator = "'" + indicatorName + "'";
		}
		return indicator;
	}

	protected NSDictionary createAjaxOptions() {
		NSMutableArray ajaxOptionsArray = new NSMutableArray();
		ajaxOptionsArray.addObject(new AjaxOption("tokens", AjaxOption.STRING_ARRAY));
		ajaxOptionsArray.addObject(new AjaxOption("frequency", AjaxOption.NUMBER));
		ajaxOptionsArray.addObject(new AjaxOption("minChars", AjaxOption.NUMBER));
		ajaxOptionsArray.addObject(new AjaxOption("indicator", indicator(), AjaxOption.SCRIPT));
		ajaxOptionsArray.addObject(new AjaxOption("updateElement", AjaxOption.SCRIPT));
		ajaxOptionsArray.addObject(new AjaxOption("afterUpdateElement", AjaxOption.SCRIPT));
		ajaxOptionsArray.addObject(new AjaxOption("onShow", AjaxOption.SCRIPT));
		ajaxOptionsArray.addObject(new AjaxOption("fullSearch", AjaxOption.BOOLEAN));
		ajaxOptionsArray.addObject(new AjaxOption("partialSearch", AjaxOption.BOOLEAN));
		ajaxOptionsArray.addObject(new AjaxOption("defaultValue", AjaxOption.STRING));
		ajaxOptionsArray.addObject(new AjaxOption("select", AjaxOption.STRING));
		ajaxOptionsArray.addObject(new AjaxOption("autoSelect", AjaxOption.BOOLEAN));
		ajaxOptionsArray.addObject(new AjaxOption("choices", AjaxOption.NUMBER));
		ajaxOptionsArray.addObject(new AjaxOption("partialChars", AjaxOption.NUMBER));
		ajaxOptionsArray.addObject(new AjaxOption("ignoreCase", AjaxOption.BOOLEAN));
		ajaxOptionsArray.addObject(new AjaxOption("activateOnFocus", AjaxOption.BOOLEAN));
		NSMutableDictionary options = AjaxOption.createAjaxOptionsDictionary(ajaxOptionsArray, this);
		return options;
	}

	/**
	 * Overridden to add the initialization javascript for the auto completer.
	 */
	public void appendToResponse(WOResponse res, WOContext ctx) {
		super.appendToResponse(res, ctx);
		boolean isDisabled = hasBinding("disabled") && ((Boolean) valueForBinding("disabled")).booleanValue();
		if ( !isDisabled ) {
			boolean isLocal = hasBinding("isLocal") && ((Boolean) valueForBinding("isLocal")).booleanValue();
			if (isLocal) {
				StringBuffer str = new StringBuffer();
				boolean isLocalSharedList = hasBinding("isLocalSharedList") && ((Boolean) valueForBinding("isLocalSharedList")).booleanValue();
				String listJS = null;
				if (isLocalSharedList) {
					String varName = (String) valueForBinding("localSharedVarName");
					NSMutableDictionary userInfo = AjaxUtils.mutableUserInfo(res);
					if (userInfo.objectForKey(varName) == null) {
						String ljs = listeJS();
						AjaxUtils.addScriptCodeInHead(res, ctx, "var " + varName + " = " + ljs + ";");
						userInfo.setObjectForKey(ljs, varName);
					}
					listJS = varName;
				} else {
					listJS = listeJS();
				}
				str.append("<script type=\"text/javascript\">\n// <![CDATA[\n");
				str.append("new MTAutocompleter.Local('");
				str.append(fieldName);
				str.append("','");
				str.append(divName);
				str.append("',");
				str.append(listJS);
				str.append(",");
				AjaxOptions.appendToBuffer(createAjaxOptions(), str, ctx);
				str.append(");\n// ]]>\n</script>\n");
				res.appendContentString(String.valueOf(str));
			} else {
				String actionUrl = AjaxUtils.ajaxComponentActionUrl(ctx);
				AjaxUtils.appendScriptHeader(res);
				res.appendContentString("new Request.Autocompleter('"+fieldName+"', '"+divName+"', '"+actionUrl+"', ");
				AjaxOptions.appendToResponse(createAjaxOptions(), res, ctx);
				res.appendContentString(");");
				AjaxUtils.appendScriptFooter(res);
			}
		}
	}	

	String listeJS() {
		StringBuffer str = new StringBuffer();
		str.append("new Array(");
		NSArray list = (NSArray) valueForBinding("list");
		int max = list.count();
		String cnt = "";
		boolean hasItem = hasBinding("item");
		for (int i = 0; i < max; i++) {
			Object ds = list.objectAtIndex(i);
			if (i > 0) {
				str.append(",");
			}
			str.append("\n\"");
			if (hasItem) {
				setValueForBinding(ds, "item");
			}
			Object displayValue = valueForBinding("displayString", valueForBinding("item", ds));
			str.append(displayValue.toString());
			// TODO: We should escape the javascript string delimiter (") to keep the javascript interpreter happy.
			//str.append(displayValue.toString().replaceAll("\"", "\\\\\\\\\"")); // doesn't work
			str.append(cnt);
			str.append("\"");
		}
		str.append(")");
		return String.valueOf(str);
	}

	public String stringValue() {
		String strValue = null;
		if (hasBinding("selection")) {
			Object selection = valueForBinding("selection");
			if (selection != null) {
				if (hasBinding("displayString")) {
					setValueForBinding(selection, "item");
					strValue = displayStringForValue(valueForBinding("value"));
				}
				else {
					strValue = String.valueOf(selection);
				}
			}
			else
				strValue = (String) valueForBinding("value");
		}
		else if (hasBinding("value")) {
			strValue = (String) valueForBinding("value");
		}
		return strValue;
	}

	protected String displayStringForValue(Object value) {
		Object displayValue = valueForBinding("displayString", valueForBinding("item", value));
		String displayString = displayValue == null ? null : displayValue.toString();
		return displayString;
	}

	protected int maxItems() {
		int maxItems = ERXValueUtilities.intValueWithDefault(valueForBinding("maxItems"), 50);
		return maxItems;
	}

	public void setStringValue(String strValue) {
		if (hasBinding("selection")) {
			Object selection = null;
			if (strValue != null) {
				NSArray values = (NSArray) valueForBinding("list");
				int maxItems = maxItems();
				int itemsCount = 0;
				for(Enumeration e = values.objectEnumerator(); e.hasMoreElements() && itemsCount++ < maxItems;) {
					Object value = e.nextElement();
					setValueForBinding(value, "item");
					String displayString = displayStringForValue(value);
					if (ERXStringUtilities.stringEqualsString(displayString, strValue)) {
						selection = value;
						break;
					}
				}
			}
			setValueForBinding(selection, "selection");
		}
		setValueForBinding(strValue, "value");
	}

	public void takeValuesFromRequest(WORequest request, WOContext context) {
		super.takeValuesFromRequest(request, context);
	}

	protected void appendItemToResponse(Object value, WOElement child, boolean hasItem, WOResponse response, WOContext context) {
		response.appendContentString("<li>");
		if(hasItem && child != null) {
			setValueForBinding(value, "item");
			context._setCurrentComponent(parent());
			child.appendToResponse(response, context);
			context._setCurrentComponent(this);
		} else {
			if(hasItem) {
				setValueForBinding(value, "item");
			}
			response.appendContentString(displayStringForValue(value));
		}
		response.appendContentString("</li>");
	}

	/**
	 * Handles the Ajax request. Checks for the form value in the edit field,
	 * pushes it up to the parent and pulls the "list" binding. The parent is
	 * responsible for returning a list with some items that match the current value.
	 */
	public WOActionResults handleRequest(WORequest request, WOContext context) {
		// String inputString = request.contentString();

		String fieldValue = context.request().stringFormValueForKey(fieldName);
		setValueForBinding(fieldValue, "value");

		WOResponse response = AjaxUtils.createResponse(request, context);
		response.appendContentString("<ul>");

		int maxItems = maxItems();
		int itemsCount = 0;
		Object values = valueForBinding("list");
		WOElement child = _childTemplate();
		boolean hasItem = hasBinding("item");
		if (values instanceof NSArray) {
			for(Enumeration valueEnum = ((NSArray)values).objectEnumerator(); valueEnum.hasMoreElements() && itemsCount++ < maxItems;) {
				appendItemToResponse(valueEnum.nextElement(), child, hasItem, response, context);
			}
		}
		else if (values instanceof List) {
			for(Iterator iter = ((List)values).iterator(); iter.hasNext() && itemsCount++ < maxItems;) {
				appendItemToResponse(iter.next(), child, hasItem, response, context);
			}
		}
		response.appendContentString("</ul>");
		return response;

	}

	public String zcontainerName() {
		return "ZContainer" + divName;
	}

}
