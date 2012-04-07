package er.extensions.eof.qualifiers;

import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSPropertyListSerialization;

import er.extensions.eof.ERXQ;

import junit.framework.TestCase;

public class ERXQTest extends TestCase {
	public void testExtractKeyValueQualifiers() {
		EOQualifier qualifier = null;
		NSArray<EOKeyValueQualifier> found = ERXQ.extractKeyValueQualifiers(qualifier);
		assertNotNull(found);
		assertTrue(found.isEmpty());
		
		qualifier = ERXQ.is("anyKey", "a");
		found = ERXQ.extractKeyValueQualifiers(qualifier);
		assertNotNull(found);
		assertEquals(1, found.count());
		assertEquals(qualifier, found.get(0));
		
		EOQualifier notQualifier = ERXQ.not(qualifier);
		found = ERXQ.extractKeyValueQualifiers(notQualifier);
		assertNotNull(found);
		assertEquals(1, found.count());
		assertEquals(qualifier, found.get(0));
		
		EOQualifier andQualifier = ERXQ.and(qualifier, notQualifier);
		found = ERXQ.extractKeyValueQualifiers(andQualifier);
		assertNotNull(found);
		assertEquals(2, found.count());
		assertTrue(found.contains(qualifier));
		
		EOQualifier orQualifier = ERXQ.or(qualifier, notQualifier, andQualifier);
		found = ERXQ.extractKeyValueQualifiers(orQualifier);
		assertNotNull(found);
		assertEquals(4, found.count());
		assertTrue(found.contains(qualifier));
	}
	
	public void testReplaceQualifierWithQualifier() {
		EOQualifier oldQualifier = ERXQ.is("age", "99");
		EOQualifier qualifier = ERXQ.and(ERXQ.contains("name", "o"), ERXQ.or(oldQualifier, ERXQ.is("haircolor", "black")));
		EOQualifier newQualifier = ERXQ.is("age", "100");
		NSArray data = NSPropertyListSerialization.arrayForString("({\"age\"=\"99\"; \"name\"=\"John\"; \"haircolor\"=\"brown\";},"
				+ "{\"age\"=\"100\"; \"name\"=\"Robert\"; \"haircolor\"=\"brown\";})");
		
		NSArray filtered = ERXQ.filtered(data, qualifier);
		assertEquals(1, filtered.count());
		assertEquals("99", ((NSDictionary)filtered.get(0)).valueForKey("age"));
		
		EOQualifier replacedQualifier = ERXQ.replaceQualifierWithQualifier(qualifier, oldQualifier, newQualifier);
		assertNotSame(qualifier, replacedQualifier);
		
		NSArray replacedFiltered = ERXQ.filtered(data, replacedQualifier);
		assertEquals(1, replacedFiltered.count());
		assertEquals("100", ((NSDictionary)replacedFiltered.get(0)).valueForKey("age"));
	}
}
