package com.example.gemini.data.files

import android.net.Uri
import androidx.core.content.FileProvider

class BoopIDEFileProvider : FileProvider() {
    override fun getType(uri: Uri): String? = super.getType(uri)

    override fun getTypeAnonymous(uri: Uri): String? = super.getTypeAnonymous(uri)
}
