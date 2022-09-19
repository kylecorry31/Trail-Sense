package com.kylecorry.trail_sense.weather.domain.clouds

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.csv.CSVConvert
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.algebra.Matrix
import com.kylecorry.sol.math.algebra.columns
import com.kylecorry.sol.math.algebra.rowMatrix
import com.kylecorry.sol.math.classifiers.IClassifier
import com.kylecorry.sol.math.classifiers.NeuralNetwork
import org.junit.Test

class CloudNeuralNetworkTrainer {

    @Test
    fun train() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = LocalFiles.read(context, "debug/clouds/clouds.csv")
        val csv = CSVConvert.parse(file)

        val x = mutableListOf<Matrix>()
        val y = mutableListOf<Matrix>()

        csv.shuffled().forEach {
            y.add(rowMatrix(values = oneHot(it[0].toInt(), 10)))
            // Remove label and bias
            x.add(rowMatrix(values = it.subList(1, it.size - 1).map { it.toFloat() }.toFloatArray()))
        }

        val nn = NeuralNetwork(
            listOf(
                NeuralNetwork.Layer.softmax(x[0].columns(), y[0].columns())
            )
        )

        nn.fit(
            x,
            y,
            1000,
            learningRate = 0.1f,
            batchSize = 5
        ) { error, epoch ->
            if (epoch % 20 == 0) {
                println("$epoch: $error")
            }
        }

        // Record training data
        LocalFiles.write(
            context,
            "debug/clouds/nn.csv",
            nn.dump().joinToString("\n") { it.format() }
        )

        println(score(nn, x, y))
    }

    private fun score(classifier: IClassifier, x: List<Matrix>, y: List<Matrix>): Float {
        var correct = 0
        x.zip(y).forEach {
            val pred = SolMath.argmax(classifier.classify(it.first[0].toList()))
            val actual = SolMath.argmax(it.second[0].toList())
            if (pred == actual) {
                correct++
            }
        }
        return correct / x.size.toFloat()
    }


    private fun oneHot(value: Int, classes: Int): FloatArray {
        return FloatArray(classes) { if (it == value) 1f else 0f }
    }

}