package com.kylecorry.trail_sense.tools.packs.infrastructure

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.trail_sense.R
import com.kylecorry.trailsensecore.domain.packs.Pack
import java.lang.Exception

class PackTableMigrationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val repo = PackRepo.getInstance(context)
            val items = repo.getItemsFromPackAsync(0)

            if (items.isEmpty()) {
                return Result.success()
            }

            val pack = Pack(0, context.getString(R.string.default_pack_name))
            val packId = repo.addPack(pack)

            val newItems = items.map { it.copy(packId = packId) }
            newItems.forEach {
                repo.addItem(it)
            }

        } catch (e: Exception) {
            // Could not migrate
            return Result.failure()
        }
        return Result.success()
    }

}