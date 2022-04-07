package com.kylecorry.trail_sense.tools.maps.infrastructure.create

import android.content.Context
import android.net.Uri
import com.kylecorry.trail_sense.shared.alerts.ILoadingIndicator
import com.kylecorry.trail_sense.shared.extensions.onIO
import com.kylecorry.trail_sense.shared.extensions.onMain
import com.kylecorry.trail_sense.tools.maps.domain.Map
import com.kylecorry.trail_sense.tools.maps.infrastructure.IMapRepo

class CreateMapFromUriCommand(
    private val context: Context,
    private val repo: IMapRepo,
    private val uri: Uri,
    private val loadingIndicator: ILoadingIndicator
): ICreateMapCommand {
    override suspend fun execute(): Map? = onIO {
        onMain {
            loadingIndicator.show()
        }
        val type = context.contentResolver.getType(uri)
        val map = if (type == "application/pdf") {
            CreateMapFromPDFCommand(context, repo).execute(uri)
        } else {
            CreateMapFromImageCommand(context, repo).execute(uri)
        }
        onMain {
            loadingIndicator.hide()
        }
        map
    }
}