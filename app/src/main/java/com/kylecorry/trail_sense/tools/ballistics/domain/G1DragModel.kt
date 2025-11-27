package com.kylecorry.trail_sense.tools.ballistics.domain

/*
* Calculated from https://www.jbmballistics.com/ballistics/downloads/text/olin.txt
* Convert v to meters, then multiple v by the G1 column to get acceleration
*/
class G1DragModel(bc: Float = 1f) : TabulatedDragModel(bc) {
    override val dragTable: Map<Float, Float> = mapOf(
        121.92f to 2.145792f,
        152.4f to 3.23088f,
        182.88f to 4.553712f,
        213.36f to 6.272784f,
        243.84f to 8.802624f,
        274.32f to 12.975336f,
        304.8f to 20.78736f,
        335.28f to 34.332672f,
        365.76f to 50.73091f,
        396.24f to 65.894714f,
        426.72f to 79.75397f,
        457.2f to 92.94876f,
        487.68f to 105.777794f,
        518.16f to 118.34775f,
        548.64f to 130.85065f,
        579.12f to 143.21637f,
        609.6f to 155.63087f,
        640.08f to 168.14902f,
        670.56f to 180.85004f,
        701.04f to 193.62724f,
        731.52f to 206.947f,
        762.0f to 220.5228f,
        792.48f to 234.49483f,
        822.96f to 249.10999f,
        853.44f to 264.31036f,
        883.92f to 280.02585f,
        914.4f to 296.53992f,
        944.88f to 313.79465f,
        975.36f to 331.6224f,
        1005.84f to 350.2335f,
        1036.32f to 369.75897f,
        1066.8f to 390.12875f,
        1097.28f to 411.26056f,
        1127.76f to 433.1726f,
        1158.24f to 455.88327f,
        1188.72f to 479.05417f,
        1219.2f to 502.92f,
        1249.68f to 527.4899f,
        1280.16f to 553.0291f,
        1310.64f to 579.0408f,
        1341.12f to 605.6498f,
    )
}