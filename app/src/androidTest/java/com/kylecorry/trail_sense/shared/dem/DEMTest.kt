package com.kylecorry.trail_sense.shared.dem

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.andromeda.files.CacheFileSystem
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.test_utils.TestStatistics.assertQuantile
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class DEMTest {

    private data class Model(
        val path: String?,
        val maxQuantile50Error: Float,
        val maxQuantile90Error: Float
    )

    private val models = listOf(
        Model(null, 11f, 60f),
        Model("dem/dem-0.3.0-low.zip", 11f, 40f),
        Model("dem/dem-0.3.0-medium.zip", 8f, 24f),
        Model("dem/dem-0.3.0-high.zip", 3f, 16f),
    )

    @Test
    fun getElevation() {
        for (model in models) {
            println("Testing ${model.path}")
            verify(model)
        }
    }

    private fun verify(model: Model) {
        val assets = AssetFileSystem(InstrumentationRegistry.getInstrumentation().context)
        val cache = CacheFileSystem(context)
        if (model.path != null && !assets.list("dem").contains(model.path.replace("dem/", ""))) {
            println("Skipping ${model.path} because it does not exist in the assets")
            return
        }
        runBlocking {
            if (model.path != null) {
                cache.outputStream("dem.zip").use { output ->
                    assets.stream(model.path).use { input ->
                        input.copyTo(output)
                    }
                }

                DigitalElevationModelLoader().load(cache.getUri("dem.zip"))
            } else {
                DigitalElevationModelLoader().clear()
            }
        }

        /*
            Generated with https://open-meteo.com

            ESA - EUsers, who, in their research, use the Copernicus DEM, are requested to use the following DOI when citing the data source in their publications:

            https://doi.org/10.5270/ESA-c5d3d65
         */
        val tests = listOf(
            Coordinate(-53.07979488142377, -69.82266083916082) to 278f,
            Coordinate(-1.759464681558832, -43.699916457444544) to 0f,
            Coordinate(-1.863999169330853, 7.435321407866706) to 0f,
            Coordinate(71.99685259757918, -117.24625201096833) to 272f,
            Coordinate(56.66136153208147, 22.76500217343102) to 113f,
            Coordinate(51.58738297002013, 34.01168664391231) to 152f,
            Coordinate(41.13364857625773, 28.156775382390066) to 61f,
            Coordinate(12.633025210984627, -49.73977542074829) to 0f,
            Coordinate(12.258920970688745, 136.0059544077002) to 0f,
            Coordinate(48.97455038195359, 92.5961445820823) to 1324f,
            Coordinate(21.152011648911927, 80.5203258711745) to 434f,
            Coordinate(35.86077234111581, -142.4398632076948) to 0f,
            Coordinate(-19.027846928970572, 39.052328929295385) to 0f,
            Coordinate(73.11535780588885, 125.7956936050409) to 4f,
            Coordinate(59.56635344174386, 27.003893177422412) to 0f,
            Coordinate(30.43387266228966, 0.8131585581722724) to 498f,
            Coordinate(-32.595487649671014, 134.45527866734415) to 52f,
            Coordinate(-11.353405559929477, 17.496223311338703) to 1217f,
            Coordinate(-22.07281720107582, 141.24827298184627) to 298f,
            Coordinate(46.393049523235085, -118.5649229474406) to 362f,
            Coordinate(-25.102204665410042, 120.77948706139105) to 611f,
            Coordinate(-12.197564882930621, 40.5765570346739) to 8f,
            Coordinate(33.686756725193376, -88.3144817920569) to 108f,
            Coordinate(51.497372324689486, -74.80713783013877) to 324f,
            Coordinate(65.66066010507798, 32.65317993075454) to 113f,
            Coordinate(51.82560875535491, -116.76804813585494) to 1554f,
            Coordinate(-39.558623071057994, -72.58453331570115) to 377f,
            Coordinate(-26.729081316396574, 135.86482985833587) to 83f,
            Coordinate(52.83915965580065, 131.47815754703043) to 526f,
            Coordinate(0.5553740192400767, -67.54889431472007) to 93f,
            Coordinate(45.089656854604065, 4.779890106240362) to 377f,
            Coordinate(61.674616184987826, 30.606830060492) to 45f,
            Coordinate(-14.217184262517499, 142.24171999422748) to 63f,
            Coordinate(-9.153433694794636, 19.386548644814035) to 1142f,
            Coordinate(36.576381005129186, 41.31798532826366) to 361f,
            Coordinate(39.00939619801157, 48.888315795325106) to -26f,
            Coordinate(33.80555836261198, 39.65528236935597) to 639f,
            Coordinate(-33.57112123147901, -70.50307671933501) to 1179f,
            Coordinate(46.40448196234921, 16.817246951896067) to 144f,
            Coordinate(11.79855438768957, -15.126571035229418) to 16f,
            Coordinate(51.05703310721128, 122.48272697695587) to 1001f,
            Coordinate(55.555154412123755, 36.37495757455227) to 219f,
            Coordinate(-31.15394448763873, 117.49214386885495) to 336f,
            Coordinate(36.190465342548436, 38.918615721265624) to 274f,
            Coordinate(-24.6637248398342, 126.15819020174355) to 401f,
            Coordinate(5.206199330473901, 30.97740383811174) to 592f,
            Coordinate(31.001595514313706, -3.9945390410893484) to 688f,
            Coordinate(54.6652262511887, 28.247575832565985) to 175f,
            Coordinate(31.180632422010913, 113.74500587799604) to 61f,
            Coordinate(51.310960513954036, 0.8467700188661862) to 49f,
            Coordinate(-32.41693647903417, 148.96199870530143) to 424f,
            Coordinate(43.990883375099045, 18.66097878261476) to 916f,
            Coordinate(58.497702105101574, 13.197234545214574) to 56f,
            Coordinate(-3.279460010963799, 13.820514826228372) to 626f,
            Coordinate(72.94534898460735, 116.94555103116897) to 44f,
            Coordinate(8.688293253118836, 12.408507196271763) to 391f,
            Coordinate(11.01546774220044, 39.0407117761944) to 2625f,
            Coordinate(44.56926457468421, -90.21107336796567) to 366f,
            Coordinate(37.43527200079942, -81.60606575316933) to 489f,
            Coordinate(27.074696808162624, 103.18668088861003) to 2336f,
            Coordinate(47.29078666755537, 7.781045756314683) to 455f,
            Coordinate(28.125909012915088, 26.28673884781833) to 121f,
            Coordinate(-26.427126986333967, 146.43721285982105) to 351f,
            Coordinate(-25.729777491895796, 131.1017367855487) to 605f,
            Coordinate(41.121086081880364, 1.1723053829407615) to 32f,
            Coordinate(48.46844600516276, 23.00326989752233) to 444f,
            Coordinate(62.318151093494, 26.06336487423676) to 113f,
            Coordinate(42.772561297693606, -98.60618662250602) to 493f,
            Coordinate(16.574130792148352, 45.204915112528916) to 1325f,
            Coordinate(-24.21453708362263, -50.922339036641176) to 779f,
            Coordinate(46.018410653740276, 53.053021921817944) to -28f,
            Coordinate(64.36571074614366, 33.029259232755955) to 118f,
            Coordinate(15.934626238008498, 43.55330995581259) to 1220f,
            Coordinate(53.55842529807604, 14.88517334418609) to 29f,
            Coordinate(-33.774248684932516, -68.10385479789883) to 632f,
            Coordinate(-20.44632386383372, 122.65726931368985) to 231f,
            Coordinate(-22.02001372997942, 148.72627200710485) to 163f,
            Coordinate(46.80672465739879, 11.619574684613035) to 1763f,
            Coordinate(43.996671415039955, 143.44758355811965) to 280f,
            Coordinate(50.64995106861453, 29.513020597798544) to 142f,
            Coordinate(76.72571154874746, 103.73186718073723) to 278f,
            Coordinate(63.25771289818552, 84.53708857532082) to 80f,
            Coordinate(53.718325349291604, -76.93228791603525) to 172f,
            Coordinate(-15.571119029692735, 127.02119737420304) to 419f,
            Coordinate(68.11762466879578, 123.02473573856385) to 115f,
            Coordinate(58.82088336946802, 35.20510631399381) to 159f,
            Coordinate(50.93970857226319, 24.367311443890394) to 200f,
            Coordinate(64.39086610833817, 13.508312687956323) to 507f,
            Coordinate(55.05125400011624, 94.50832026293008) to 714f,
            Coordinate(-12.42091751637238, 143.19449282736136) to 73f,
            Coordinate(46.199335188887865, 17.32986507014149) to 137f,
            Coordinate(-26.42312469194527, 123.05072648373346) to 440f,
            Coordinate(-34.49021020324375, 144.5472295936553) to 82f,
            Coordinate(12.917135367417075, -4.819499186834777) to 281f,
            Coordinate(-30.17444888866283, 137.0056560340795) to 109f,
            Coordinate(60.501707454151045, -153.3226487797337) to 1121f,
            Coordinate(63.704397818148095, -133.05309532551377) to 954f,
            Coordinate(43.647619177809645, -72.7550532260068) to 730f,
            Coordinate(24.925649893578097, -8.829460501857874) to 301f,
            Coordinate(19.665019, -155.834663) to 1986f
        )

        val errors = tests.map { test ->
            val actual = runBlocking { DEM.getElevation(test.first) }
            assertNotNull(actual)
            actual!!.meters().distance - test.second
        }

        assertQuantile(errors, model.maxQuantile50Error, 0.5f, model.path ?: "Built-in")
        assertQuantile(errors, model.maxQuantile90Error, 0.9f, model.path ?: "Built-in")
    }

}