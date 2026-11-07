package com.kylecorry.trail_sense.shared.data

import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.trail_sense.shared.andromeda_temp.ByteRangeReader
import com.kylecorry.trail_sense.shared.andromeda_temp.ChannelByteRangeReader
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import java.io.Closeable

/**
 * Reads an asset, which must be stored uncompressed in the APK (noCompress in the build file)
 */
class AssetByteRangeReader(path: String) : ByteRangeReader by ChannelByteRangeReader({
    val descriptor = DependencyRegistry.get<FileSubsystem>().openAssetDescriptor(path)
    val stream = descriptor.createInputStream()
    ChannelByteRangeReader.OpenChannel(stream.channel, descriptor.startOffset, descriptor.length, Closeable {
        stream.close()
        descriptor.close()
    })
})
