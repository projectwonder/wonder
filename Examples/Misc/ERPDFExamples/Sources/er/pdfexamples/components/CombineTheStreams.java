package er.pdfexamples.components;

import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOAssociation;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOElement;
import com.webobjects.appserver.WOResponse;
import com.webobjects.appserver._private.WODynamicGroup;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;

import er.extensions.appserver.ERXWOContext;
import er.extensions.components.ERXComponent;
import er.pdf.ERPDFMerge;

public class CombineTheStreams extends ERXComponent {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(CombineTheStreams.class);
	



	public String filename = "combined_result.pdf";
	public NSMutableArray<InputStream> pdfsToCombine;
	public NSData data;




	public CombineTheStreams(WOContext context) {
		super(context);
		pdfsToCombine = new NSMutableArray<InputStream>();
	}


	

	public void combinedResponseAsPdf(WOResponse response, WOContext context) {
		
		
		
		
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ERPDFMerge.concatPDFs(pdfsToCombine, output, false);
		data = new NSData(output.toByteArray());
		
		
		response.setHeader("inline; filename=\"" + filename + "\"", "content-disposition");
		response.setHeader("application/pdf", "Content-Type");
		response.setHeader(String.valueOf(data.length()), "Content-Length");
		response.setContent(data);
	}
	
	/**
	 * combine the NSData elements to one pdf file
	 */
	@Override
	public void appendToResponse(WOResponse response, WOContext context) {
		super.appendToResponse(response, context);
		combinedResponseAsPdf(response, context);
	}


}
