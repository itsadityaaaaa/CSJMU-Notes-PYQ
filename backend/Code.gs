/**
 * CSJMU Notes & PYQ - Google Apps Script Backend
 * 
 * Free Google Drive Storage & Document Approval API
 * Keeps Firebase on the 100% Free Spark Plan (No Firebase Storage, No Cloud Functions required)
 * 
 * Google Drive Folder Structure:
 * NotesApp/
 *    ├── Pending/        (Uploaded documents waiting for admin moderation)
 *    ├── Approved/       (Published documents accessible to students)
 *    └── metadata.json   (Cloud record of all submissions and approved resources)
 */

const ROOT_FOLDER_NAME = "NotesApp";
const PENDING_FOLDER_NAME = "Pending";
const APPROVED_FOLDER_NAME = "Approved";
const METADATA_FILE_NAME = "metadata.json";

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

      case "getPending":
      case "getPendingSubmissions":
        return handleGetPendingSubmissions();

      case "getApproved":
      case "getApprovedResources":
        return handleGetApprovedResources();

      case "getAll":
      case "getAllSubmissions":
        return handleGetAllSubmissions();

      default:
        return createJsonResponse({ success: false, error: "Invalid action: " + action }, 400);
    }
  } catch (err) {
    return createJsonResponse({ success: false, error: err.toString() }, 500);
  }
}

/**
 * Health check and GET query endpoint.
 */
function doGet(e) {
  try {
    const action = (e && e.parameter && e.parameter.action) ? e.parameter.action : "";
    if (action === "getPending" || action === "getPendingSubmissions") {
      return handleGetPendingSubmissions();
    }
    if (action === "getApproved" || action === "getApprovedResources") {
      return handleGetApprovedResources();
    }
    if (action === "getAll" || action === "getAllSubmissions") {
      return handleGetAllSubmissions();
    }

    return createJsonResponse({
      status: "online",
      service: "CSJMU Notes Google Drive Backend",
      timestamp: new Date().toISOString()
    });
  } catch (err) {
    return createJsonResponse({ success: false, error: err.toString() }, 500);
  }
}

/**
 * 1. Uploads a user PDF to NotesApp/Pending/ folder in Google Drive.
 * Also stores the submission record into NotesApp/metadata.json.
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

  // Set viewing permission to anyone with link for students & admin preview
  file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);

  const fileId = file.getId();
  const fileUrl = "https://drive.google.com/file/d/" + fileId + "/view?usp=sharing";
  const previewUrl = "https://drive.google.com/file/d/" + fileId + "/preview";
  const downloadUrl = "https://drive.google.com/uc?export=download&id=" + fileId;
  const now = new Date().getTime();
  const expiresAt = now + (3 * 24 * 60 * 60 * 1000); // 3-day expiration

  // Persist submission record in NotesApp/metadata.json
  const submissionRecord = {
    id: data.submissionId || ("sub_" + Utilities.getUuid()),
    title: data.title || fileName,
    description: data.description || "",
    collegeId: data.collegeId || "",
    courseId: data.courseId || "",
    courseName: data.courseName || "",
    academicYear: Number(data.academicYear || 1),
    semester: Number(data.semester || 1),
    subjectId: data.subjectId || "",
    subjectName: data.subjectName || "",
    resourceType: data.resourceType || "NOTES",
    examYear: data.examYear || "",
    fileUri: fileUrl,
    driveFileId: fileId,
    driveFileUrl: fileUrl,
    previewUrl: previewUrl,
    downloadUrl: downloadUrl,
    fileName: fileName,
    fileSizeFormatted: data.fileSizeFormatted || formatBytes(file.getSize()),
    fileMimeType: mimeType,
    fileSizeBytes: file.getSize(),
    contributorName: data.contributorName || "Student",
    contributorEmail: data.contributorEmail || "",
    status: "PENDING",
    isApproved: false,
    adminFeedback: "",
    submittedTimestamp: now,
    reviewedTimestamp: null,
    expiresAt: expiresAt
  };

  const allMetadata = loadAllMetadata();
  // Filter out any duplicate with same ID
  const filtered = allMetadata.filter(function(item) {
    return item.id !== submissionRecord.id && item.driveFileId !== fileId;
  });
  filtered.push(submissionRecord);
  saveAllMetadata(filtered);

  return createJsonResponse({
    success: true,
    fileId: fileId,
    fileUrl: fileUrl,
    previewUrl: previewUrl,
    downloadUrl: downloadUrl,
    fileName: file.getName(),
    sizeBytes: file.getSize(),
    submission: submissionRecord
  });
}

/**
 * 2. Approves document: moves file from NotesApp/Pending to NotesApp/Approved.
 * Updates metadata.json to status = "APPROVED".
 */
function handleApproveDocument(data) {
  const fileId = data.fileId;
  const submissionId = data.submissionId;

  if (!fileId && !submissionId) {
    return createJsonResponse({ success: false, error: "fileId or submissionId is required" }, 400);
  }

  const allMetadata = loadAllMetadata();
  let matchedItem = null;

  for (let i = 0; i < allMetadata.length; i++) {
    if ((fileId && allMetadata[i].driveFileId === fileId) || (submissionId && allMetadata[i].id === submissionId)) {
      allMetadata[i].status = "APPROVED";
      allMetadata[i].isApproved = true;
      allMetadata[i].reviewedTimestamp = new Date().getTime();
      if (data.adminNotes) {
        allMetadata[i].adminFeedback = data.adminNotes;
      }
      matchedItem = allMetadata[i];
      break;
    }
  }

  const targetFileId = (matchedItem && matchedItem.driveFileId) ? matchedItem.driveFileId : fileId;

  if (targetFileId) {
    try {
      const file = DriveApp.getFileById(targetFileId);
      const pendingFolder = getOrCreateSubFolder(PENDING_FOLDER_NAME);
      const approvedFolder = getOrCreateSubFolder(APPROVED_FOLDER_NAME);

      // Move file from Pending to Approved
      approvedFolder.addFile(file);
      pendingFolder.removeFile(file);

      // Ensure anyone with link can view
      file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
    } catch (e) {
      // File may already have been moved
    }
  }

  saveAllMetadata(allMetadata);

  return createJsonResponse({
    success: true,
    fileId: targetFileId,
    message: "File approved and moved to Approved folder",
    item: matchedItem
  });
}

/**
 * 3. Rejects document: deletes file from Google Drive and marks REJECTED in metadata.
 */
function handleRejectDocument(data) {
  const fileId = data.fileId;
  const submissionId = data.submissionId;

  if (!fileId && !submissionId) {
    return createJsonResponse({ success: false, error: "fileId or submissionId is required" }, 400);
  }

  const allMetadata = loadAllMetadata();
  let matchedItem = null;

  for (let i = 0; i < allMetadata.length; i++) {
    if ((fileId && allMetadata[i].driveFileId === fileId) || (submissionId && allMetadata[i].id === submissionId)) {
      allMetadata[i].status = "REJECTED";
      allMetadata[i].isApproved = false;
      allMetadata[i].reviewedTimestamp = new Date().getTime();
      if (data.reason || data.adminNotes) {
        allMetadata[i].adminFeedback = data.reason || data.adminNotes;
      }
      matchedItem = allMetadata[i];
      break;
    }
  }

  const targetFileId = (matchedItem && matchedItem.driveFileId) ? matchedItem.driveFileId : fileId;

  if (targetFileId) {
    try {
      const file = DriveApp.getFileById(targetFileId);
      file.setTrashed(true); // Move to trash / delete from Drive
    } catch (err) {
      // File may already be deleted
    }
  }

  saveAllMetadata(allMetadata);

  return createJsonResponse({
    success: true,
    fileId: targetFileId,
    message: "Rejected file deleted from Google Drive"
  });
}

/**
 * 4. Automatically deletes pending files older than 3 days.
 * Daily Time-Driven Trigger can run this automatically.
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

  // Update metadata.json records
  const allMetadata = loadAllMetadata();
  let updatedCount = 0;
  for (let i = 0; i < allMetadata.length; i++) {
    if (allMetadata[i].status === "PENDING" && allMetadata[i].submittedTimestamp < threeDaysAgo) {
      allMetadata[i].status = "EXPIRED";
      allMetadata[i].isApproved = false;
      updatedCount++;
    }
  }
  if (updatedCount > 0) {
    saveAllMetadata(allMetadata);
  }

  return createJsonResponse({
    success: true,
    deletedCount: deletedCount,
    updatedMetadataCount: updatedCount,
    message: "Cleaned up " + deletedCount + " expired pending documents"
  });
}

/**
 * 5. Returns all pending submissions waiting for admin moderation.
 */
function handleGetPendingSubmissions() {
  const all = loadAllMetadata();
  const now = new Date().getTime();
  const pending = all.filter(function(item) {
    return item.status === "PENDING" && (!item.expiresAt || item.expiresAt > now);
  });

  return createJsonResponse({
    success: true,
    count: pending.length,
    submissions: pending
  });
}

/**
 * 6. Returns all approved documents publicly visible to students.
 */
function handleGetApprovedResources() {
  const all = loadAllMetadata();
  const approved = all.filter(function(item) {
    return item.status === "APPROVED" || item.isApproved === true;
  });

  return createJsonResponse({
    success: true,
    count: approved.length,
    resources: approved
  });
}

/**
 * 7. Returns all submissions.
 */
function handleGetAllSubmissions() {
  const all = loadAllMetadata();
  return createJsonResponse({
    success: true,
    count: all.length,
    submissions: all
  });
}

// ==================== Storage Helpers ====================

function getOrCreateRootFolder() {
  const rootIterator = DriveApp.getFoldersByName(ROOT_FOLDER_NAME);
  if (rootIterator.hasNext()) {
    return rootIterator.next();
  }
  return DriveApp.createFolder(ROOT_FOLDER_NAME);
}

function getOrCreateSubFolder(subFolderName) {
  const rootFolder = getOrCreateRootFolder();
  const subIterator = rootFolder.getFoldersByName(subFolderName);
  if (subIterator.hasNext()) {
    return subIterator.next();
  }
  return rootFolder.createFolder(subFolderName);
}

function getMetadataFile() {
  const root = getOrCreateRootFolder();
  const files = root.getFilesByName(METADATA_FILE_NAME);
  if (files.hasNext()) {
    return files.next();
  }
  return root.createFile(METADATA_FILE_NAME, "[]", MimeType.PLAIN_TEXT);
}

function loadAllMetadata() {
  try {
    const file = getMetadataFile();
    const content = file.getBlob().getDataAsString();
    const parsed = JSON.parse(content || "[]");
    return Array.isArray(parsed) ? parsed : [];
  } catch (e) {
    return [];
  }
}

function saveAllMetadata(list) {
  try {
    const file = getMetadataFile();
    file.setContent(JSON.stringify(list, null, 2));
  } catch (e) {
    // Retry once if file handle was stale
    const root = getOrCreateRootFolder();
    const files = root.getFilesByName(METADATA_FILE_NAME);
    if (files.hasNext()) {
      files.next().setContent(JSON.stringify(list, null, 2));
    }
  }
}

function formatBytes(bytes) {
  if (bytes > 1024 * 1024) {
    return (bytes / (1024 * 1024)).toFixed(1) + " MB";
  }
  if (bytes > 1024) {
    return Math.round(bytes / 1024) + " KB";
  }
  return bytes + " B";
}

function createJsonResponse(data, statusCode) {
  return ContentService.createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
}
