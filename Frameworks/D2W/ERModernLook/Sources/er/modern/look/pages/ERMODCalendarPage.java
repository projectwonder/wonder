package er.modern.look.pages;

import com.webobjects.appserver.WOContext;

import er.directtoweb.pages.templates.ERD2WCalendarPageTemplate;

public class ERMODCalendarPage extends ERD2WCalendarPageTemplate {
  /**
   * Do I need to update serialVersionUID?
   * See section 5.6 <cite>Type Changes Affecting Serialization</cite> on page 51 of the 
   * <a href="http://java.sun.com/j2se/1.4/pdf/serial-spec.pdf">Java Object Serialization Spec</a>
   */
  private static final long serialVersionUID = 1L;

	public ERMODCalendarPage(WOContext wocontext) {
		super(wocontext);
	}

}
