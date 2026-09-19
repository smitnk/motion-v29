package com.smitnk.motioncanvas.importer

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri

object ImageSequenceImporter {
    fun decode(resolver: ContentResolver, uris: List<Uri>) = uris.mapNotNull { uri ->
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }
}