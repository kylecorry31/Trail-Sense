package com.kylecorry.trail_sense.tools.comms.messaging

import android.content.Context
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tool
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCategory
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

object LocalMessagingToolRegistration : ToolRegistration {
    override fun getTool(context: Context): Tool {
        // TODO: Have an open action rather than navigation ID
        return Tool(
            Tools.LOCAL_MESSAGING,
            context.getString(R.string.local_messaging),
            R.drawable.ic_send,
            R.id.fragmentLocalMessaging,
            ToolCategory.Communication,
            isExperimental = true,
            isAvailable = {
                Package.isPackageInstalled(it, "com.kylecorry.trail_sense_comms")
            }
        )
    }
}