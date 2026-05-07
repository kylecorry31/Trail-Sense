package com.kylecorry.trail_sense.tools.offline_maps

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapFile
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapFileType
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.OfflineMapFileRepo
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.Instant

class ToolOfflineMapsTest : ToolTestBase(Tools.OFFLINE_MAPS) {

    @Test
    fun verifyBasicFunctionality() {
        // TODO: To run on staging builds another solution to populating maps will need to be used
        if (AutomationLibrary.packageName != null) {
            return
        }

        // Offline maps disclaimer
        optional { clickOk() }

        hasText(R.id.title, string(R.string.offline_maps))
        hasText(string(R.string.no_offline_maps))

        canOpenOfflineMapFilePicker()
        canCreateOfflineMapGroup()

        seedOfflineMap("Second Map")
        seedOfflineMap("Test Map")
        waitFor { hasText("Test Map") }
        hasText("Second Map")
        hasText("Mapsforge")

        canToggleOfflineMapVisibility()
        canViewOfflineMap()
        canRenameOfflineMapGroup()
        canSearchOfflineMaps()
        canRenameOfflineMap()
        canEditOfflineMapAttribution()
        canMoveOfflineMap()
        canDeleteOfflineMapGroup()
        canDeleteOfflineMap()
    }

    private fun canOpenOfflineMapFilePicker() {
        click(R.id.add_btn)
        click(string(R.string.offline_map_file))

        // The file picker is opened
        isNotVisible(R.id.add_btn)

        waitFor {
            waitFor {
                back()
            }
            isVisible(R.id.title)
        }
    }

    private fun canCreateOfflineMapGroup() {
        click(R.id.add_btn)
        click(string(R.string.group))

        input(string(R.string.name), "Test Group", index = 1)
        clickOk()
        hasText("Test Group")
        hasText("0 maps")
    }

    private fun canToggleOfflineMapVisibility() {
        click(com.kylecorry.andromeda.views.R.id.trailing_icon_btn)
        click(com.kylecorry.andromeda.views.R.id.trailing_icon_btn)
    }

    private fun canViewOfflineMap() {
        click("Test Map")
        hasText("Test Map")
        click(R.id.zoom_in_btn)
        click(R.id.zoom_out_btn)
        back()
    }

    private fun canRenameOfflineMapGroup() {
        clickListItemMenu(string(R.string.rename), index = 0)
        input("Test Group", "Test Group 2")
        clickOk()
        hasText("Test Group 2")
        hasText("0 maps")
    }

    private fun canSearchOfflineMaps() {
        input(R.id.searchbox, "Second")
        hasText("Second Map")
        not { hasText("Test Map") }
        input(R.id.searchbox, "")
        hasText("Test Group 2")
        hasText("Test Map")
    }

    private fun canRenameOfflineMap() {
        clickListItemMenu(string(R.string.rename), index = 2)
        input("Test Map", "Test Map 2")
        clickOk()
        hasText("Test Map 2")
    }

    private fun canEditOfflineMapAttribution() {
        clickListItemMenu(string(R.string.attribution), index = 2)
        input(string(R.string.attribution), "Test Attribution")
        clickOk()
    }

    private fun canMoveOfflineMap() {
        clickListItemMenu(string(R.string.move_to), index = 2)
        click("Test Group 2")
        click(string(R.string.move))
        hasText("1 map")
        not { hasText("Test Map 2") }
    }

    private fun canDeleteOfflineMapGroup() {
        clickListItemMenu(string(R.string.delete), index = 0)
        clickOk()
        not { hasText("Test Group 2") }
        not { hasText("Test Map 2") }
    }

    private fun canDeleteOfflineMap() {
        clickListItemMenu(string(R.string.delete))
        clickOk()
        not { hasText("Second Map") }
        hasText(string(R.string.no_offline_maps))
    }

    private fun seedOfflineMap(name: String) = runBlocking {
        val files = getAppService<FileSubsystem>()
        files.getDirectory("offline_maps", true)
        val filename = name.lowercase().replace(" ", "-")
        val path = "offline_maps/$filename.map"
        files.get(path, true).writeBytes(ByteArray(0))

        getAppService<OfflineMapFileRepo>().add(
            OfflineMapFile(
                0,
                name,
                OfflineMapFileType.Mapsforge,
                path,
                0,
                Instant.now(),
                null,
                null,
                visible = true
            )
        )
    }
}
