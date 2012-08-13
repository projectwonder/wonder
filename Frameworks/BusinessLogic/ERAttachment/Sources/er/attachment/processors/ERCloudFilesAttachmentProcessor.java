package er.attachment.processors;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.HttpException;

import com.rackspacecloud.client.cloudfiles.FilesAuthorizationException;
import com.rackspacecloud.client.cloudfiles.FilesClient;
import com.rackspacecloud.client.cloudfiles.FilesException;
import com.rackspacecloud.client.cloudfiles.FilesInvalidNameException;
import com.rackspacecloud.client.cloudfiles.FilesNotFoundException;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSTimestamp;

import er.attachment.ERAttachmentRequestHandler;
import er.attachment.model.ERCloudFilesAttachment;
import er.extensions.concurrency.ERXAsyncQueue;
import er.extensions.eof.ERXEC;
import er.extensions.foundation.ERXExceptionUtilities;
import er.extensions.foundation.ERXProperties;

/**
 * ERCloudFilesAttachmentProcessor implements storing attachments in RackSpace's CloudFiles
 * service. For more information about configuring an ERCloudFilesAttachmentProcessor,
 * see the top level documentation.
 * 
 * @property er.attachment.[configurationName].cf.container
 * @property er.attachment.cf.container
 * @property er.attachment.[configurationName].cf.key
 * @property er.attachment.cf.key
 * @property er.attachment.[configurationName].cf.username
 * @property er.attachment.cf.username
 * @property er.attachment.[configurationName].cf.secretAccessKey
 * @property er.attachment.cf.secretAccessKey
 * @author mschrag
 */
public class ERCloudFilesAttachmentProcessor extends
		ERAttachmentProcessor<ERCloudFilesAttachment> {
	public static final String CF_URL = "https://auth.api.rackspacecloud.com/v1.0";

	private ERCloudFilesUploadQueue _queue;

	public ERCloudFilesAttachmentProcessor() {
		_queue = new ERCloudFilesUploadQueue();
		_queue.start();
	}

	@Override
	public ERCloudFilesAttachment _process(EOEditingContext editingContext,
			File uploadedFile, String recommendedFileName, String mimeType,
			String configurationName, String ownerID, boolean pendingDelete) {
		boolean proxy = true;
		String proxyStr = ERXProperties.stringForKey("er.attachment."
				+ configurationName + ".cf.proxy");
		if (proxyStr == null) {
			proxyStr = ERXProperties.stringForKey("er.attachment.cf.proxy");
		}
		if (proxyStr != null) {
			proxy = Boolean.parseBoolean(proxyStr);
		}

		String bucket = ERXProperties.decryptedStringForKey("er.attachment."
				+ configurationName + ".cf.container");
		if (bucket == null) {
			bucket = ERXProperties
					.decryptedStringForKey("er.attachment.cf.container");
		}
    if (bucket == null) {
      throw new IllegalArgumentException("There is no 'er.attachment."
          + configurationName
          + ".cf.container' or 'er.attachment.cf.container' property set.");
    }

		String keyTemplate = ERXProperties.stringForKey("er.attachment."
				+ configurationName + ".cf.key");
		if (keyTemplate == null) {
			keyTemplate = ERXProperties.stringForKey("er.attachment.cf.key");
		}
		if (keyTemplate == null) {
			keyTemplate = "${pk}${ext}";
		}

		ERCloudFilesAttachment attachment = ERCloudFilesAttachment.createERCloudFilesAttachment(
				editingContext, Boolean.FALSE, new NSTimestamp(), mimeType,
				recommendedFileName, proxy,
				Integer.valueOf((int) uploadedFile.length()), null);
		if (delegate() != null) {
			delegate().attachmentCreated(this, attachment);
		}
		try {
      String key = ERAttachmentProcessor._parsePathTemplate(attachment,
          keyTemplate, recommendedFileName);
      attachment.setWebPath("/" + bucket + "/" + key);
		  attachment.setConfigurationName(configurationName);
			attachment._setPendingUploadFile(uploadedFile, pendingDelete);
		} catch (RuntimeException e) {
			attachment.delete();
			if (pendingDelete) {
				uploadedFile.delete();
			}
			throw e;
		}

		return attachment;
	}

	@Override
	public InputStream attachmentInputStream(ERCloudFilesAttachment attachment)
			throws IOException {
		try {
      return attachment.cloudFilesConnection().getObjectAsStream(attachment.container(), attachment.key());
    }
    catch (FilesAuthorizationException e) {
      throw new IOException(e);
    }
    catch (FilesInvalidNameException e) {
      throw new IOException(e);
    }
    catch (FilesNotFoundException e) {
      throw new IOException(e);
    }
    catch (HttpException e) {
      throw new IOException(e);
    }
	}

	@Override
	public String attachmentUrl(ERCloudFilesAttachment attachment, WORequest request,
			WOContext context) {
		String attachmentUrl = attachment.cfPath();

		if (attachment.proxied()) {

			if (!attachment.acl().equals("private")) {
				log.warn("You are proxying an CloudFiles attachment but do not have the attachment configured for private acl. This likely means the CloudFiles attachment is publically readable via CloudFiles, and therefore I'm wondering why you are proxying it through your app. You should either change the acl configuraiton for this attachment to 'private', or why not just serve the attachment up directly from CloudFiles ?");
			}

			// drop the AWS domain
			try {
				attachmentUrl = (new URL(attachmentUrl)).getPath();
				attachmentUrl = "id/" + attachment.primaryKey() + attachmentUrl;
				attachmentUrl = context.urlWithRequestHandlerKey(
						ERAttachmentRequestHandler.REQUEST_HANDLER_KEY,
						attachmentUrl, null);
			} catch (MalformedURLException e) {
				log.fatal(
						"attachment.cfPath() is returning something that isn't a valid URl. This is a bt strange. I'm going to reutrn it in it's raw format which will result in either a 'url cannot be found' error or may result in a 403 from CloudFiles.",
						e);
			}

		}

		return attachmentUrl;

	}

	@Override
	public void deleteAttachment(ERCloudFilesAttachment attachment)
			throws MalformedURLException, IOException {
		FilesClient conn = attachment.cloudFilesConnection();
		String bucket = attachment.container();
		String key = attachment.key();
		try {
      conn.deleteObject(bucket, key);
    }
    catch (FilesNotFoundException e) {
      throw new IOException("Failed to delete '" + bucket + "/" + key
          + "' to CloudFiles: Error " + e.getHttpStatusCode()
          + ": " + e.getHttpStatusMessage());
    }
    catch (FilesException e) {
      throw new IOException("Failed to delete '" + bucket + "/" + key
          + "' to CloudFiles: Error " + e.getHttpStatusCode()
          + ": " + e.getHttpStatusMessage());
    }
    catch (HttpException e) {
      throw new IOException("Failed to delete '" + bucket + "/" + key
          + "' to CloudFiles: Error " + e.getMessage());
    }
	}

	@Override
	public void attachmentInserted(ERCloudFilesAttachment attachment) {
		super.attachmentInserted(attachment);
		_queue.enqueue(attachment);
	}

	public void performUpload(File uploadedFile, String originalFileName,
			String bucket, String key, String mimeType,
			ERCloudFilesAttachment attachment) throws MalformedURLException,
			IOException {
		try {
			FilesClient conn = attachment.cloudFilesConnection();
			FileInputStream attachmentFileInputStream = new FileInputStream(
					uploadedFile);
			BufferedInputStream attachmentInputStream = new BufferedInputStream(
					attachmentFileInputStream);
			try {
				try {
          conn.storeObjectAs(bucket, uploadedFile, mimeType, key);
          URL pathToFile = new URL(conn.getStorageURL() + "/" + bucket + "/" + key);
          attachment.setCfPath(pathToFile.toExternalForm());
          attachment.setWebPath(pathToFile.getPath());
        }
        catch (FilesException e) {
          throw new IOException("Failed to write '" + bucket + "/"
              + key + "' to CloudFiles: Error "
              + e.getHttpStatusCode() + ": "
              + e.getMessage());
        }
        catch (HttpException e) {
          throw new IOException("Failed to write '" + bucket + "/"
              + key + "' to CloudFiles: Error "
              + e.getMessage());
        }
			} finally {
				attachmentInputStream.close();
			}
		} finally {
			if (attachment._isPendingDelete()) {
				uploadedFile.delete();
			}
		}

	}

	public class ERCloudFilesQueueEntry {
		private File _uploadedFile;
		private ERCloudFilesAttachment _attachment;

		public ERCloudFilesQueueEntry(File uploadedFile, ERCloudFilesAttachment attachment) {
			_uploadedFile = uploadedFile;
			_attachment = attachment;
		}

		public File uploadedFile() {
			return _uploadedFile;
		}

		public ERCloudFilesAttachment attachment() {
			return _attachment;
		}
	}

	public class ERCloudFilesUploadQueue extends ERXAsyncQueue<ERCloudFilesQueueEntry> {
		private EOEditingContext _editingContext;

		public ERCloudFilesUploadQueue() {
			super("ERS3AsyncQueue");
			_editingContext = ERXEC.newEditingContext();
		}

		public void enqueue(ERCloudFilesAttachment attachment) {
			_editingContext.lock();
			try {
			  ERCloudFilesAttachment localAttachment = attachment
						.localInstanceIn(_editingContext);
				ERCloudFilesQueueEntry entry = new ERCloudFilesQueueEntry(
						attachment._pendingUploadFile(), localAttachment);
				enqueue(entry);
			} finally {
				_editingContext.unlock();
			}
		}

		@Override
		public void process(ERCloudFilesQueueEntry object) {
		  ERCloudFilesAttachment attachment = object.attachment();

			File uploadedFile = object.uploadedFile();
			if (uploadedFile != null && uploadedFile.exists()) {
				String bucket;
				String key;
				String mimeType;
				String configurationName;
				String originalFileName = null;

				_editingContext.lock();
				try {
					bucket = attachment.container();
					key = attachment.key();
					mimeType = attachment.mimeType();
					configurationName = attachment.configurationName();
					if (proxyAsAttachment(attachment)) {
						originalFileName = attachment.originalFileName();
					}
				} finally {
					_editingContext.unlock();
				}

				try {
					ERCloudFilesAttachmentProcessor.this
							.performUpload(uploadedFile, originalFileName,
									bucket, key, mimeType, attachment);

					_editingContext.lock();
					try {
						attachment.setAvailable(Boolean.TRUE);
						_editingContext.saveChanges();
					} finally {
						_editingContext.unlock();
					}
					if (ERCloudFilesAttachmentProcessor.this.delegate() != null) {
						ERCloudFilesAttachmentProcessor.this.delegate()
								.attachmentAvailable(
										ERCloudFilesAttachmentProcessor.this,
										attachment);
					}
				} catch (Throwable t) {
					if (ERCloudFilesAttachmentProcessor.this.delegate() != null) {
						ERCloudFilesAttachmentProcessor.this.delegate()
								.attachmentNotAvailable(
										ERCloudFilesAttachmentProcessor.this,
										attachment,
										ERXExceptionUtilities.toParagraph(t));
					}
					ERAttachmentProcessor.log.error("Failed to upload '"
							+ uploadedFile + "' to CloudFiles.", t);
				} finally {
					if (attachment._isPendingDelete()) {
						uploadedFile.delete();
					}
				}
			} else {
				if (ERCloudFilesAttachmentProcessor.this.delegate() != null) {
					ERCloudFilesAttachmentProcessor.this.delegate()
							.attachmentNotAvailable(
									ERCloudFilesAttachmentProcessor.this,
									attachment,
									"Missing attachment file '" + uploadedFile
											+ "'.");
				}
				ERAttachmentProcessor.log.error("Missing attachment file '"
						+ uploadedFile + "'.");
			}
		}
	}

}
