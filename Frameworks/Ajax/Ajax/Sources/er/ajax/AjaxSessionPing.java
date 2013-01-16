package er.ajax;

import com.webobjects.appserver.*;
import com.webobjects.foundation.*;

import er.extensions.appserver.ERXHttpStatusCodes;
import er.extensions.appserver.ERXResponse;

/**
 * Simple component to ping the session in the background.  It can do two things.  The first is
 * to execute JavaScript if the session is no longer valid. The default action is to close the window
 * that the ping came from.  The second thing it can do is to keep the session alive.
 * This can be useful if you want the session to not time out on particular pages.  It should be
 * used with caution as it can prevent scheduled restarts if the user leaves the browser window open.
 *
 * @binding frequency the period between pings of the application (optional, default 60 seconds)
 * @binding keepSessionAlive true if session should be checked out to reset timeout when the
 *              application is pinged (optional, default false)
 * @binding parameters optional URL parameter string appended when application is pinged (optional, no default)
 * @binding onFailure function to execute if the session has expired or other HTTP error code returned from
 *              ping (optional, default "function(response) { window.close();}")
 * @binding asynchronous true if the ping should be made asynchronously (optional, default true)
 * @binding evalScripts true if the ping results may contain JavaScript that should be evaluated (optional, default false)
 * @binding method the HTTP request method to use for the ping (optional, default "get")
 *
 * @author chill
 */
public class AjaxSessionPing extends AjaxDynamicElement {

    public AjaxSessionPing(String name, NSDictionary associations, WOElement children) {
        super(name, associations, children);
    }

    /**
     * Appends script to start Ajax.ActivePeriodicalUpdater to the response.
     */
    public void appendToResponse(WOResponse response, WOContext context) {
    	super.appendToResponse(response, context);
        WOComponent component = context.component();
        response.appendContentString("<script>var AjaxSessionPinger = new Ajax.ActivePeriodicalUpdater('AjaxSessionPinger', '");

        if (booleanValueForBinding("keepSessionAlive", false, component)) {
            response.appendContentString(context.directActionURLForActionNamed("AjaxSessionPing$Action/pingSessionAndKeepAlive", null));
        } else {
            response.appendContentString(context.directActionURLForActionNamed("AjaxSessionPing$Action/pingSession", null));
        }

        response.appendContentString("', ");
        AjaxOptions.appendToResponse(createAjaxOptions(component), response, context);
        response.appendContentString(");</script>");
    }

    /**
     * Gathers the bindings into an AjaxOptions dictionary.
     *
     * @param component the component to evaluate the bindings in
     * @return the bindings in the form of an AjaxOptions dictionary
     */
    public NSDictionary createAjaxOptions(WOComponent component) {
        NSMutableArray ajaxOptionsArray = new NSMutableArray();
        ajaxOptionsArray.addObject(new AjaxOption("asynchronous", Boolean.TRUE, AjaxOption.BOOLEAN));
        ajaxOptionsArray.addObject(new AjaxOption("evalScripts", Boolean.FALSE, AjaxOption.BOOLEAN));
        ajaxOptionsArray.addObject(new AjaxOption("frequency", Integer.valueOf(60), AjaxOption.NUMBER));
        ajaxOptionsArray.addObject(new AjaxOption("method", "get", AjaxOption.STRING));
        ajaxOptionsArray.addObject(new AjaxOption("onFailure", "function(response) { window.close();}", AjaxOption.SCRIPT));
        ajaxOptionsArray.addObject(new AjaxOption("parameters", AjaxOption.STRING));
        NSMutableDictionary options = AjaxOption.createAjaxOptionsDictionary(ajaxOptionsArray, component, associations());
        return options;
    }

    /**
     * Unused.
     */
    public WOActionResults handleRequest(WORequest request, WOContext context) {
        return null;
    }


    /**
     * Uses Prototype and Wonder
     */
    protected void addRequiredWebResources(WOResponse response, WOContext context) {
        addScriptResourceInHead(context, response, "prototype.js");
        addScriptResourceInHead(context, response, "wonder.js");
    }


    /**
     * Internal WODirectAction subclass to handle the request from AjaxSessionPing.
     */
    public static class Action extends WODirectAction {

        public Action(WORequest request) {
            super(request);
        }

        /**
         * If there is a session, returns a response with a success (200) code.  If there is
         * not a session, returns a response with a failure (300) code so that the
         * ActivePeriodicalUpdater can call the onFailure call back.
         *
         * @return bare HTTP response with status set
         */
        public WOActionResults pingSessionAction() {
            ERXResponse response = new ERXResponse();
            if (existingSession() != null) {
                session();
            } else {
            	response.setStatus(ERXHttpStatusCodes.MULTIPLE_CHOICES); // CHECKME is that really the appropriate status code?
            }
            return response;
        }

        /**
         * Same as pingSessionAction, but also checks out session to keep it alive.
         *
         * @see #pingSessionAction
         * @return bare HTTP response with status set
         */
        public WOActionResults pingSessionAndKeepAliveAction() {
            if (existingSession() != null) {
                session();
            }
            return pingSessionAction();
        }

    	/**
    	 * There has been a long standing problem with exceptions thrown from a
    	 * DirectAction. If the session has been referenced it is not checked back
    	 * in and the application will deadlock immediately if it is not dispatching
    	 * requests concurrently. If it is dispatching concurrently, just that one
    	 * session will deadlock. To avoid this, implement the DirectAction method
    	 * performAction() along these lines:
    	 */
    	@Override
    	public WOActionResults performActionNamed(String name)
    	{
    		try
    		{
    			return super.performActionNamed(name);
    		}
    		catch (Exception e)
    		{
    			return WOApplication.application().handleException(e, context());
    		}
    		catch (Throwable t)
    		{
    			return WOApplication.application().handleException(new NSForwardException(t), context());
    		}
    	}
    }

}