package com.kylecorry.trail_sense.tools.pedometer.infrastructure.persistence

import android.content.Context
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.tools.pedometer.domain.StepCountBucket
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackingPeriod
import com.kylecorry.trail_sense.tools.pedometer.domain.abstractions.IStepTrackerRepository
import java.time.Instant

class StepTrackerRepo private constructor(private val dao: StepTrackerDao) : IStepTrackerRepository {

    override suspend fun getStepTrackingPeriods(): List<StepTrackingPeriod> = onIO {
        dao.getStepTrackingPeriods().map { it.toStepTrackingPeriod(getBuckets(it.id)) }
    }

    override suspend fun getOpenStepTrackingPeriod(): StepTrackingPeriod? = onIO {
        dao.getOpenStepTrackingPeriod()?.let {
            it.toStepTrackingPeriod(getBuckets(it.id))
        }
    }

    override suspend fun upsertStepTrackingPeriod(period: StepTrackingPeriod): Long = onIO {
        val entity = StepTrackingPeriodEntity.from(period)
        dao.upsert(entity)
    }

    override suspend fun deleteStepTrackingPeriod(period: StepTrackingPeriod) = onIO {
        dao.delete(StepTrackingPeriodEntity.from(period))
    }

    override suspend fun getStepCountBuckets(
        startTime: Instant,
        endTime: Instant
    ): List<StepCountBucket> = onIO {
        dao.getStepCountBuckets(startTime, endTime).map { it.toStepCountBucket() }
    }

    override suspend fun upsertStepCountBucket(bucket: StepCountBucket): Long = onIO {
        val entity = StepCountBucketEntity.from(bucket)
        dao.upsert(entity)
    }

    override suspend fun deleteBucketsInPeriod(periodId: Long) = onIO {
        dao.deleteBucketsInPeriod(periodId)
    }

    override suspend fun deleteBucketsOlderThan(endTime: Instant) = onIO {
        dao.deleteBucketsOlderThan(endTime) > 0
    }

    override suspend fun deleteEmptyClosedPeriods() = onIO {
        dao.deleteEmptyClosedPeriods() > 0
    }

    private suspend fun getBuckets(periodId: Long): List<StepCountBucket> {
        return dao.getStepCountBuckets(periodId).map { it.toStepCountBucket() }
    }

    companion object {
        private var instance: StepTrackerRepo? = null

        @Synchronized
        fun getInstance(context: Context): StepTrackerRepo {
            if (instance == null) {
                instance = StepTrackerRepo(
                    AppDatabase.getInstance(context.applicationContext).stepTrackerDao()
                )
            }
            return instance!!
        }
    }
}
