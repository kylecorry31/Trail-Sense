package com.kylecorry.trail_sense.weather.domain.clouds

import androidx.test.platform.app.InstrumentationRegistry
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.csv.CSVConvert
import com.kylecorry.andromeda.files.LocalFiles
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.algebra.*
import com.kylecorry.sol.math.classifiers.IClassifier
import com.kylecorry.sol.math.classifiers.LogisticRegressionClassifier
import org.junit.Test
import kotlin.random.Random

class CloudTrainer {

    @Test
    fun train() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = LocalFiles.read(context, "debug/clouds/clouds.csv")
        val csv = CSVConvert.parse(file)

        val x = mutableListOf<List<Float>>()
        val y = mutableListOf<Int>()

        val random = Random(1)
        csv.shuffled(random).forEach {
            y.add(it[0].toInt())
            // Remove label
            x.add(it.subList(1, it.size).map { it.toFloat() })
        }
        val samples = (x.size * 0.8).toInt()
        val trainX = x.take(samples)
        val trainY = y.take(samples)
        val testX = x.takeLast(x.size - samples)
        val testY = y.takeLast(x.size - samples)

        val clf = LogisticRegressionClassifier.fromWeights(
            createMatrix(
                x.first().size,
                10
            ) { _, _ -> Math.random().toFloat() * 0.1f })

        val loss = mutableListOf<List<Any>>()

        clf.fitClasses(
            trainX,
            trainY,
            5000,
            learningRate = 0.1f,
            batchSize = 5
        ) { error, epoch ->
            loss.add(listOf(epoch, error))
        }

        // Record training data
        val rows =
            clf.dump().joinToString(",\n") { "arrayOf(${it.joinToString(",") { "${it}f" }})" }
        LocalFiles.write(
            context,
            "debug/clouds/weights.txt",
            "arrayOf(\n$rows\n)"
        )

        LocalFiles.write(
            context,
            "debug/clouds/loss.csv",
            CSVConvert.toCSV(loss)
        )

        println()
        val trainConfusion = confusion(clf, 10, trainX, trainY)
        val testConfusion = confusion(clf, 10, testX, testY)
        println("TRAIN")
        println("Accuracy: ${accuracy(trainConfusion)}")
        println("F1 (Average): ${f1(trainConfusion)}")
        println("F1 (Weighted): ${f1(trainConfusion, true)}")
        println(" ")
        println("F1")
        printF1Matrix(trainConfusion)
        println(" ")
        println(" ")
        printConfusion(trainConfusion)
        println(" ")
        println(" ")
        println("Test")
        println("Accuracy: ${accuracy(testConfusion)}")
        println("F1 (Average): ${f1(testConfusion)}")
        println("F1 (Weighted): ${f1(testConfusion, true)}")
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
        val f1s = confusion.mapIndexed { index, _ -> index to f1(confusion, index) }
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

    private fun confusion(
        classifier: IClassifier,
        classes: Int,
        x: List<List<Float>>,
        y: List<Int>
    ): Matrix {
        val matrix = createMatrix(classes, classes, 0f)
        x.zip(y).forEach {
            val pred = SolMath.argmax(classifier.classify(it.first))
            matrix[pred][it.second]++
        }
        return matrix
    }

    private fun accuracy(confusion: Matrix): Float {
        val total = confusion.sum()
        val correct = confusion.multiply(identityMatrix(confusion.rows())).sum()
        return correct / total
    }

    private fun f1(confusion: Matrix, weighted: Boolean = false): Float {
        val all = confusion.sum()
        val weight = 1 / confusion.rows().toFloat()
        return confusion.mapIndexed { index, _ ->
            val total = confusion.transpose()[index].sum()
            f1(confusion, index) * if (weighted) total / all else weight
        }.sum()
    }

    private fun f1(confusion: Matrix, label: Int): Float {
        val predicted = confusion[label]
        val actual = confusion.transpose()[label]

        val tp = confusion[label][label]
        val fp = predicted.sum() - tp
        val fn = actual.sum() - tp
        val precision = tp / (tp + fp)
        val recall = tp / (tp + fn)
        val f = (2 * precision * recall) / (precision + recall)
        if (f.isNaN()) {
            return 0f
        }
        return f
    }


}