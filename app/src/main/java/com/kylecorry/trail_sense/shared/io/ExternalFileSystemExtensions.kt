package com.kylecorry.trail_sense.shared.io

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.kylecorry.andromeda.files.ExternalFileSystem

fun ExternalFileSystem.createFile(
    context: Context,
    treeUri: Uri,
    name: String,
    mimeType: String
): Uri? {
    return DocumentFile.fromTreeUri(context, treeUri)
        ?.createFile(mimeType, name)?.uri
}

fun ExternalFileSystem.deleteFile(context: Context, treeUri: Uri, name: String): Boolean {
    return deleteFile(context, getDocumentUri(context, treeUri, name) ?: return false)
}

fun ExternalFileSystem.deleteFile(context: Context, uri: Uri): Boolean {
    return DocumentFile.fromSingleUri(context, uri)?.delete() == true
}

fun ExternalFileSystem.getDocumentUri(context: Context, treeUri: Uri, name: String): Uri? {
    return DocumentFile.fromTreeUri(context, treeUri)?.findFile(name)?.uri
}

fun ExternalFileSystem.listFiles(context: Context, uri: Uri): List<DocumentFile> {
    return DocumentFile.fromTreeUri(context, uri)?.listFiles()?.toList() ?: emptyList()
}

fun ExternalFileSystem.hasWritePermission(context: Context, treeUri: Uri, name: String): Boolean {
    return hasWritePermission(context, getDocumentUri(context, treeUri, name) ?: return false)
}

fun ExternalFileSystem.hasWritePermission(context: Context, uri: Uri): Boolean {
    return DocumentFile.fromTreeUri(context, uri)?.canWrite() == true
}

fun ExternalFileSystem.hasReadPermission(context: Context, treeUri: Uri, name: String): Boolean {
    return hasReadPermission(context, getDocumentUri(context, treeUri, name) ?: return false)
}

fun ExternalFileSystem.hasReadPermission(context: Context, uri: Uri): Boolean {
    return DocumentFile.fromTreeUri(context, uri)?.canRead() == true
}