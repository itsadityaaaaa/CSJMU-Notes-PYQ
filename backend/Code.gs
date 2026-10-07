/**
 * CSJMU Notes & PYQ - Google Apps Script Backend
 * 
 * Free Google Drive Storage & Document Approval API
 * Keeps Firebase on the 100% Free Spark Plan (No Firebase Storage, No Cloud Functions required)
 * 
 * Google Drive Folder Structure:
 * NotesApp/
 *    ├── Pending/   (Uploaded documents waiting for admin moderation)
 *    └── Approved/  (Published documents accessible to students)
 */

const ROOT_FOLDER_NAME = "NotesApp";
const PENDING_FOLDER_NAME = "Pending";
const APPROVED_FOLDER_NAME = "Approved";

/**
 * Handle incoming POST requests from the Android Application.
 */
function doPost(e) {
  try {
    if (!e || !e.postData || !e.postData.contents) {
      return createJsonResponse({ success: false, error: "Missing request payload" }, 400);
    }

    const request = JSON.parse(e.postData.contents);
    const action = request.action;

    switch (action) {
      case "uploadPending":
        return handleUploadPending(request);

      case "approveDocument":
        return handleApproveDocument(request);

      case "rejectDocument":
        return handleRejectDocument(request);

      case "cleanupExpired":
        return handleCleanupExpired();

      default:
        return createJsonResponse({ success: false, error: "Invalid action: " + action }, 400);
    }
  } catch (err) {
    return createJsonResponse({ success: false, error: err.toString() }, 500);
  }
}

/**
 * Health check or status test endpoint.
 */
function doGet(e) {
  return createJsonResponse({
    status: "online",
    service: "CSJMU Notes Google Drive Backend",
    timestamp: new Date().toISOString()
  });
}

/**
 * 1. Uploads a user PDF to NotesApp/Pending/ folder in Google Drive.
 */
function handleUploadPending(data) {
  const fileName = data.fileName || "Note_" + new Date().getTime() + ".pdf";
  const mimeType = data.mimeType || "application/pdf";
  const base64Data = data.base64Data;

  if (!base64Data) {
    return createJsonResponse({ success: false, error: "base64Data is required" }, 400);
  }

  // Enforce Max File Size: 25 MB limit
  const decodedBytes = Utilities.base64Decode(base64Data);
  if (decodedBytes.length > 25 * 1024 * 1024) {
    return createJsonResponse({ success: false, error: "File exceeds 25 MB maximum limit" }, 400);
  }

  const pendingFolder = getOrCreateSubFolder(PENDING_FOLDER_NAME);
  const blob = Utilities.newBlob(decodedBytes, mimeType, fileName);
  const file = pendingFolder.createFile(blob);

  // Set viewing permission to anyone with link for admin preview
  file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);

  const fileId = file.getId();
  const fileUrl = file.getUrl();
  const downloadUrl = file.getDownloadUrl();

  return createJsonResponse({
    success: true,
    fileId: fileId,
    fileUrl: fileUrl,
    downloadUrl: downloadUrl,
    fileName: file.getName(),
    sizeBytes: file.getSize()
  });
}

/**
 * 2. Approves document: moves file from NotesApp/Pending to NotesApp/Approved.
 */
function handleApproveDocument(data) {
  const fileId = data.fileId;
  if (!fileId) {
    return createJsonResponse({ success: false, error: "fileId is required" }, 400);
  }

  const file = DriveApp.getFileById(fileId);
  const pendingFolder = getOrCreateSubFolder(PENDING_FOLDER_NAME);
  const approvedFolder = getOrCreateSubFolder(APPROVED_FOLDER_NAME);

  // Move file from Pending to Approved
  approvedFolder.addFile(file);
  pendingFolder.removeFile(file);

  // Ensure read-only view permission for students
  file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);

  return createJsonResponse({
    success: true,
    fileId: file.getId(),
    approvedUrl: file.getUrl(),
    message: "File moved to Approved folder successfully"
  });
}

/**
 * 3. Rejects document: deletes file from Google Drive.
 */
function handleRejectDocument(data) {
  const fileId = data.fileId;
  if (!fileId) {
    return createJsonResponse({ success: false, error: "fileId is required" }, 400);
  }

  try {
    const file = DriveApp.getFileById(fileId);
    file.setTrashed(true); // Move to trash / delete
  } catch (err) {
    // File may already be removed or invalid ID
  }

  return createJsonResponse({
    success: true,
    message: "Rejected file deleted from Google Drive"
  });
}

/**
 * 4. Automatically deletes pending files older than 3 days.
 * Set up a Daily Time-Driven Trigger in Google Apps Script to run this automatically!
 */
function handleCleanupExpired() {
  const pendingFolder = getOrCreateSubFolder(PENDING_FOLDER_NAME);
  const files = pendingFolder.getFiles();
  const threeDaysAgo = new Date().getTime() - (3 * 24 * 60 * 60 * 1000);
  let deletedCount = 0;

  while (files.hasNext()) {
    const file = files.next();
    if (file.getDateCreated().getTime() < threeDaysAgo) {
      file.setTrashed(true);
      deletedCount++;
    }
  }

  return createJsonResponse({
    success: true,
    deletedCount: deletedCount,
    message: "Cleaned up " + deletedCount + " expired pending documents"
  });
}

/**
 * Helper to get or create root and subfolder.
 */
function getOrCreateSubFolder(subFolderName) {
  let rootFolder;
  const rootIterator = DriveApp.getFoldersByName(ROOT_FOLDER_NAME);
  if (rootIterator.hasNext()) {
    rootFolder = rootIterator.next();
  } else {
    rootFolder = DriveApp.createFolder(ROOT_FOLDER_NAME);
  }

  const subIterator = rootFolder.getFoldersByName(subFolderName);
  if (subIterator.hasNext()) {
    return subIterator.next();
  } else {
    return rootFolder.createFolder(subFolderName);
  }
}

function createJsonResponse(data, statusCode) {
  return ContentService.createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
}
