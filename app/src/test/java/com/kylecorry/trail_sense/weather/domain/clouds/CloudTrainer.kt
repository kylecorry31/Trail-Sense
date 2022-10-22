package com.kylecorry.trail_sense.weather.domain.clouds

import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.csv.CSVConvert
import com.kylecorry.sol.math.algebra.Matrix
import com.kylecorry.sol.math.algebra.createMatrix
import com.kylecorry.sol.math.classifiers.LogisticRegressionClassifier
import com.kylecorry.sol.math.classifiers.confusion
import com.kylecorry.sol.math.split
import com.kylecorry.sol.math.statistics.Statistics
import java.io.File
import kotlin.random.Random

class CloudTrainer {

//    @Test
    fun train() {
        val file = File("src/data/clouds.csv").readText()
        val csv = CSVConvert.parse(file)

        val x = mutableListOf<List<Float>>()
        val y = mutableListOf<Int>()

        val random = Random(1)
        csv.shuffled(random).forEach {
            y.add(it[0].toInt())
            // Remove label
            x.add(it.subList(1, it.size).map { it.toFloat() })
        }
        val split = x.zip(y).split(0.8f)
        val trainX = split.first.unzip().first
        val trainY = split.first.unzip().second
        val testX = split.second.unzip().first
        val testY = split.second.unzip().second

        val clf = LogisticRegressionClassifier.fromWeights(
            createMatrix(
                x.first().size,
                10
            ) { _, _ -> Math.random().toFloat() * 0.1f })

        val loss = mutableListOf<List<Any>>()

        clf.fitClasses(
            trainX,
            trainY,
            10000,
            learningRate = 0.2f,
            batchSize = 3
        ) { error, epoch ->
            loss.add(listOf(epoch, error))
        }

        // Record training data
        val rows =
            clf.dump().joinToString(",\n") { "arrayOf(${it.joinToString(",") { "${it}f" }})" }

        val output = File("src/data/output")
        output.mkdir()

        val weightsFile = File("src/data/output/weights.txt")
        weightsFile.writeText("arrayOf(\n$rows\n)")

        val lossFile = File("src/data/output/loss.csv")
        lossFile.writeText(CSVConvert.toCSV(loss))

        println()
        val trainConfusion = clf.confusion(10, trainX, trainY)
        val testConfusion = clf.confusion(10, testX, testY)
        println("TRAIN")
        println("Accuracy: ${Statistics.accuracy(trainConfusion)}")
        println("F1 (Average): ${Statistics.f1Score(trainConfusion)}")
        println("F1 (Weighted): ${Statistics.f1Score(trainConfusion, true)}")
        println(" ")
        println("F1")
        printF1Matrix(trainConfusion)
        println(" ")
        println(" ")
        printConfusion(trainConfusion)
        println(" ")
        println(" ")
        println("Test")
        println("Accuracy: ${Statistics.accuracy(testConfusion)}")
        println("F1 (Average): ${Statistics.f1Score(testConfusion)}")
        println("F1 (Weighted): ${Statistics.f1Score(testConfusion, true)}")
        println(" ")
        println("F1")
        printF1Matrix(testConfusion)
        println(" ")
        println(" ")
        printConfusion(testConfusion)
    }

    private fun printF1Matrix(confusion: Matrix) {
        val labels = listOf(
            "Ci",
            "Cc",
            "Cs",
            "As",
            "Ac",
            "Ns",
            "Sc",
            "Cu",
            "St",
            "Cb"
        )
        val f1s = confusion.mapIndexed { index, _ -> index to Statistics.f1Score(confusion, index) }
        println(
            f1s.joinToString("\n") {
                "${labels[it.first]}: ${
                    DecimalFormatter.format(
                        it.second,
                        2
                    )
                }"
            }
        )
    }

    private fun printConfusion(confusion: Matrix) {
        val labels = listOf(
            "Ci",
            "Cc",
            "Cs",
            "As",
            "Ac",
            "Ns",
            "Sc",
            "Cu",
            "St",
            "Cb"
        )
        println("    " + labels.joinToString("  "))
        var i = 0
        println(
            confusion.joinToString("\n") {
                labels[i++] + "  " + it.joinToString("  ") {
                    it.toInt().toString().padStart(2, ' ')
                }
            }
        )
    }

}