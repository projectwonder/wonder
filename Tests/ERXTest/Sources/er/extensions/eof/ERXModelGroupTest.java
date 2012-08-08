
package er.extensions.eof;

import junit.framework.Assert;

import com.webobjects.eoaccess.EOModelGroup;

import er.erxtest.ERXTestCase;
import er.erxtest.ERXTestUtilities;

public class ERXModelGroupTest extends ERXTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testConstructor() {
        Assert.assertNotNull(new ERXModelGroup());
    }

    public void testSettingEOModelGroupClass() {
        Assert.assertEquals("er.extensions.eof.SortOfModelGroup", EOModelGroup.defaultGroup().getClass().getName());
    }
}
