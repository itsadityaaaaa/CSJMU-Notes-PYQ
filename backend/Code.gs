/**
 * CSJMU Notes & PYQ - Google Apps Script Backend
 * 
 * Free Google Drive Storage & Document Moderation API
 * 100% Free Tier Compliant (No Firebase Storage, No Cloud Functions required)
 * 
 * Google Drive Folder Structure:
 * NotesApp/
 *    ├── Pending/        (Uploaded documents waiting for admin moderation)
 *    ├── Approved/       (Published documents accessible to students)
 *    └── metadata.json   (Single canonical cloud record of all submissions and approved resources)
 */

const ROOT_FOLDER_NAME = "NotesApp";
const PENDING_FOLDER_NAME = "Pending";
const APPROVED_FOLDER_NAME = "Approved";
const METADATA_FILE_NAME = "metadata.json";

/**
 * Handle incoming POST requests from the Android Application.
 * Protected with LockService to prevent concurrent execution race conditions.
 */
function doPost(e) {
  const lock = LockService.getScriptLock();
  try {
    lock.waitLock(30000); // Wait up to 30s for lock
  } catch (lockErr) {
    return createJsonResponse({ success: false, error: "Server busy. Please retry shortly." }, 503);
  }

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

      case "consolidate":
      case "consolidateMetadata":
        return handleConsolidateMetadata();

      case "getPending":
      case "getPendingSubmissions":
        return handleGetPendingSubmissions();

      case "getApproved":
      case "getApprovedResources":
        return handleGetApprovedResources();

      case "getAll":
      case "getAllSubmissions":
        return handleGetAllSubmissions();

      case "editDocument":
      case "updateDocument":
        return handleEditDocument(request);

      case "deleteDocument":
      case "deleteUploadedDocument":
        return handleDeleteDocument(request);

      default:
        return createJsonResponse({ success: false, error: "Invalid action: " + action }, 400);
    }
  } catch (err) {
    return createJsonResponse({ success: false, error: err.toString() }, 500);
  } finally {
    try {
      lock.releaseLock();
    } catch (e) {}
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
    if (action === "consolidate" || action === "consolidateMetadata") {
      return handleConsolidateMetadata();
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
 * 1. Uploads a user PDF/Image to NotesApp/Pending/ folder in Google Drive.
 * Stores the submission record into the single canonical NotesApp/metadata.json.
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

  // Persist submission record in canonical metadata.json
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
 * 3b. Edits an uploaded or approved document in Google Drive:
 * Updates the document metadata in canonical NotesApp/metadata.json,
 * and synchronizes the Google Drive file name and details.
 */
function handleEditDocument(data) {
  const fileId = data.fileId || data.driveFileId;
  const submissionId = data.submissionId || data.id;

  if (!fileId && !submissionId) {
    return createJsonResponse({ success: false, error: "fileId or submissionId is required" }, 400);
  }

  const allMetadata = loadAllMetadata();
  let matchedItem = null;

  for (let i = 0; i < allMetadata.length; i++) {
    const item = allMetadata[i];
    const isMatch = (fileId && item.driveFileId === fileId) ||
                    (submissionId && (item.id === submissionId || item.id === "res_" + submissionId || "res_" + item.id === submissionId));
    if (isMatch) {
      matchedItem = item;
      break;
    }
  }

  if (!matchedItem) {
    matchedItem = {
      id: submissionId || ("sub_" + Utilities.getUuid()),
      driveFileId: fileId || ""
    };
    allMetadata.push(matchedItem);
  }

  // Update fields
  if (data.title !== undefined) matchedItem.title = data.title;
  if (data.description !== undefined) matchedItem.description = data.description;
  if (data.collegeId !== undefined) matchedItem.collegeId = data.collegeId;
  if (data.courseId !== undefined) matchedItem.courseId = data.courseId;
  if (data.courseName !== undefined) matchedItem.courseName = data.courseName;
  if (data.academicYear !== undefined) matchedItem.academicYear = Number(data.academicYear);
  if (data.semester !== undefined) matchedItem.semester = Number(data.semester);
  if (data.subjectId !== undefined) matchedItem.subjectId = data.subjectId;
  if (data.subjectName !== undefined) matchedItem.subjectName = data.subjectName;
  if (data.resourceType !== undefined) matchedItem.resourceType = data.resourceType;
  if (data.examYear !== undefined) matchedItem.examYear = data.examYear;
  if (data.contributorName !== undefined) matchedItem.contributorName = data.contributorName;
  if (data.contributorEmail !== undefined) matchedItem.contributorEmail = data.contributorEmail;
  if (data.adminFeedback !== undefined) matchedItem.adminFeedback = data.adminFeedback;
  if (data.status !== undefined) {
    matchedItem.status = data.status;
    matchedItem.isApproved = (data.status === "APPROVED");
  }
  matchedItem.updatedTimestamp = new Date().getTime();

  // If Drive file exists, update file name if subject changed
  const targetFileId = matchedItem.driveFileId || fileId;
  if (targetFileId) {
    try {
      const file = DriveApp.getFileById(targetFileId);
      if (data.fileName && data.fileName.trim().length > 0) {
        file.setName(data.fileName.trim());
        matchedItem.fileName = data.fileName.trim();
      } else if (data.subjectName && data.subjectName.trim().length > 0) {
        const oldName = file.getName();
        const ext = oldName.indexOf('.') !== -1 ? oldName.split('.').pop() : "pdf";
        const newName = (data.subjectName + "_" + (data.resourceType || "NOTES") + "." + ext).replace(/[^a-zA-Z0-9._-]/g, "_");
        file.setName(newName);
        matchedItem.fileName = newName;
      }
    } catch (e) {}
  }

  saveAllMetadata(allMetadata);

  return createJsonResponse({
    success: true,
    fileId: targetFileId,
    message: "Document edited and synced to Google Drive metadata successfully",
    item: matchedItem
  });
}

/**
 * 3c. Permanently deletes an uploaded/approved document from Google Drive (trashes the file)
 * and purges it completely from NotesApp/metadata.json.
 */
function handleDeleteDocument(data) {
  const fileId = data.fileId || data.driveFileId;
  const submissionId = data.submissionId || data.id;

  if (!fileId && !submissionId) {
    return createJsonResponse({ success: false, error: "fileId or submissionId is required" }, 400);
  }

  const allMetadata = loadAllMetadata();
  let matchedFileId = fileId;
  const filtered = allMetadata.filter(function(item) {
    const isMatch = (fileId && item.driveFileId === fileId) ||
                    (submissionId && (item.id === submissionId || item.id === "res_" + submissionId || "res_" + item.id === submissionId));
    if (isMatch && item.driveFileId) {
      matchedFileId = item.driveFileId;
    }
    return !isMatch;
  });

  // Permanently trash the file in Google Drive
  if (matchedFileId) {
    try {
      const file = DriveApp.getFileById(matchedFileId);
      file.setTrashed(true);
    } catch (err) {}
  }

  saveAllMetadata(filtered);

  return createJsonResponse({
    success: true,
    fileId: matchedFileId,
    deletedFromDrive: true,
    remainingCount: filtered.length,
    message: "Document permanently deleted from Google Drive and purged from metadata"
  });
}

/**
 * 4. Automatically deletes pending files older than 3 days.
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

/**
 * 8. Manually or automatically consolidate metadata:
 * Searches for all metadata.json files in NotesApp, merges all submissions into
 * one canonical file, deletes empty "[]" duplicates, and saves the file ID.
 */
function handleConsolidateMetadata() {
  const result = consolidateAndCleanMetadata();
  return createJsonResponse({
    success: true,
    message: "Metadata consolidated successfully",
    canonicalFileId: result.fileId,
    totalRecords: result.records.length,
    deletedEmptyDuplicates: result.trashedCount,
    records: result.records
  });
}

// ==================== Atomic Storage Helpers ====================

function getOrCreateRootFolder() {
  const props = PropertiesService.getScriptProperties();
  const cachedRootId = props.getProperty("ROOT_FOLDER_ID");
  if (cachedRootId) {
    try {
      const folder = DriveApp.getFolderById(cachedRootId);
      if (folder && !folder.isTrashed()) {
        return folder;
      }
    } catch (e) {}
  }

  const rootIterator = DriveApp.getFoldersByName(ROOT_FOLDER_NAME);
  let rootFolder = null;
  while (rootIterator.hasNext()) {
    const f = rootIterator.next();
    if (!f.isTrashed()) {
      rootFolder = f;
      break;
    }
  }

  if (!rootFolder) {
    rootFolder = DriveApp.createFolder(ROOT_FOLDER_NAME);
  }

  props.setProperty("ROOT_FOLDER_ID", rootFolder.getId());
  return rootFolder;
}

function getOrCreateSubFolder(subFolderName) {
  const props = PropertiesService.getScriptProperties();
  const propKey = "SUBFOLDER_ID_" + subFolderName;
  const cachedSubId = props.getProperty(propKey);
  if (cachedSubId) {
    try {
      const folder = DriveApp.getFolderById(cachedSubId);
      if (folder && !folder.isTrashed()) {
        return folder;
      }
    } catch (e) {}
  }

  const rootFolder = getOrCreateRootFolder();
  const subIterator = rootFolder.getFoldersByName(subFolderName);
  let subFolder = null;
  while (subIterator.hasNext()) {
    const f = subIterator.next();
    if (!f.isTrashed()) {
      subFolder = f;
      break;
    }
  }

  if (!subFolder) {
    subFolder = rootFolder.createFolder(subFolderName);
  }

  props.setProperty(propKey, subFolder.getId());
  return subFolder;
}

/**
 * Atomic metadata file accessor that prevents duplicate files and consolidates existing ones.
 * Resolves the issue where 3 metadata files are created with two empty "[]".
 */
function getMetadataFile() {
  const props = PropertiesService.getScriptProperties();
  const cachedId = props.getProperty("METADATA_FILE_ID");

  if (cachedId) {
    try {
      const file = DriveApp.getFileById(cachedId);
      if (file && !file.isTrashed()) {
        return file;
      }
    } catch (e) {
      // Stale or invalid cached ID
    }
  }

  // Find, consolidate, and clean up all metadata.json files in the root folder
  const consolidated = consolidateAndCleanMetadata();
  return DriveApp.getFileById(consolidated.fileId);
}

/**
 * Finds all files named metadata.json in NotesApp, merges all items deduplicated by id,
 * trashes all empty "[]" or duplicate files, and caches the single canonical file ID.
 */
function consolidateAndCleanMetadata() {
  const props = PropertiesService.getScriptProperties();
  const root = getOrCreateRootFolder();
  const files = root.getFilesByName(METADATA_FILE_NAME);

  const allFoundFiles = [];
  const mergedMap = {};

  while (files.hasNext()) {
    const f = files.next();
    if (f.isTrashed()) continue;
    allFoundFiles.push(f);

    try {
      const content = f.getBlob().getDataAsString();
      const parsed = JSON.parse(content || "[]");
      if (Array.isArray(parsed)) {
        for (let i = 0; i < parsed.length; i++) {
          const item = parsed[i];
          const key = item.id || item.driveFileId;
          if (key && !mergedMap[key]) {
            mergedMap[key] = item;
          }
        }
      }
    } catch (err) {}
  }

  const mergedList = Object.keys(mergedMap).map(function(k) { return mergedMap[k]; });
  let canonicalFile = null;
  let trashedCount = 0;

  if (allFoundFiles.length === 0) {
    // No file exists yet, create exactly one
    canonicalFile = root.createFile(METADATA_FILE_NAME, "[]", MimeType.PLAIN_TEXT);
    canonicalFile.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
  } else {
    // Keep the first file as canonical, trash all other duplicates
    canonicalFile = allFoundFiles[0];
    canonicalFile.setContent(JSON.stringify(mergedList, null, 2));
    canonicalFile.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);

    for (let i = 1; i < allFoundFiles.length; i++) {
      try {
        allFoundFiles[i].setTrashed(true);
        trashedCount++;
      } catch (e) {}
    }
  }

  props.setProperty("METADATA_FILE_ID", canonicalFile.getId());

  return {
    fileId: canonicalFile.getId(),
    records: mergedList,
    trashedCount: trashedCount
  };
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
    // Fallback: clear stale cached ID and consolidate
    const props = PropertiesService.getScriptProperties();
    props.deleteProperty("METADATA_FILE_ID");
    const file = getMetadataFile();
    file.setContent(JSON.stringify(list, null, 2));
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
