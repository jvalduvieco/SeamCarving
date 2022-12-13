package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.min
import kotlin.math.pow

fun main(args: Array<String>) {
    val inName = args[1]
    val outName = args[3]
    val widthToRemove = args.getOrNull(5)?.toInt() ?: 0
    val heightToRemove = args.getOrNull(7)?.toInt() ?: 0

    val image = ImageIO.read(File(inName))

    var wipImage = image

    repeat((0 until widthToRemove).count()) {
        val seam = findVerticalMinimumSeam(wipImage)
        wipImage = removeVerticalSeam(wipImage, seam)
    }
    repeat((0 until heightToRemove).count()) {
        val seam = findHorizontalMinimumSeam(wipImage)
        wipImage = removeHorizontalSeam(wipImage, seam)
    }
    ImageIO.write(wipImage, "png", File(outName))
}

fun forEveryPixelTopToBottom(image: BufferedImage, callback: (x: Int, y: Int) -> Unit) {
    for (y in 0 until image.height) // Row by row fill top down
        for (x in 0 until image.width) {
            callback(x, y)
        }
}

fun forEveryPixelLeftToRight(image: BufferedImage, callback: (x: Int, y: Int) -> Unit) {
    for (x in 0 until image.width) {
        for (y in 0 until image.height) // Row by row fill top down
            callback(x, y)
    }
}

fun squareGradient(firstPixel: Color, secondPixel: Color): Double {
    return (firstPixel.red - secondPixel.red).toDouble().pow(2) +
            (firstPixel.green - secondPixel.green).toDouble().pow(2) +
            (firstPixel.blue - secondPixel.blue).toDouble().pow(2)
}

fun pixelEnergy(image: BufferedImage, x: Int, y: Int): Double {
    val maxX = image.width - 1
    val maxY = image.height - 1
    val effectiveX = when (x) {
        0 -> 1
        maxX -> image.width - 2
        else -> x
    }
    val effectiveY = when (y) {
        0 -> 1
        maxY -> image.height - 2
        else -> y
    }
    val xSquareGradient = squareGradient(
        Color(image.getRGB((effectiveX - 1).coerceIn(0, maxX), y), false),
        Color(image.getRGB((effectiveX + 1).coerceIn(0, maxX), y), false),
    )
    val ySquareGradient = squareGradient(
        Color(image.getRGB(x, (effectiveY - 1).coerceIn(0, maxY)), false),
        Color(image.getRGB(x, (effectiveY + 1).coerceIn(0, maxY)), false)
    )
    return kotlin.math.sqrt(xSquareGradient + ySquareGradient)
}

fun paintSeam(image: BufferedImage, seam: List<Pair<Int, Int>>): BufferedImage {
    seam.map { image.setRGB(it.first, it.second, Color(255, 0, 0).rgb) }
    return image
}

fun removeVerticalSeam(image: BufferedImage, seam: List<Pair<Int, Int>>): BufferedImage {
    val pendingSeam = seam.toMutableList().asReversed()
    val newImage = BufferedImage(image.width - 1, image.height, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until image.height) {
        if (y != pendingSeam.first().second) throw RuntimeException()
        for (x in 0 until image.width) {
            val seamX = pendingSeam.first().first
            val effectiveX = when {
                x == seamX -> continue
                0 <= x && x < seamX -> x
                seamX < x && x < image.width -> x - 1
                else -> throw RuntimeException()
            }
            newImage.setRGB(effectiveX, y, image.getRGB(x, y))
        }
        pendingSeam.removeFirst()
    }
    return newImage
}

fun removeHorizontalSeam(image: BufferedImage, seam: List<Pair<Int, Int>>): BufferedImage {
    val pendingSeam = seam.toMutableList().asReversed()
    val newImage = BufferedImage(image.width, image.height - 1, BufferedImage.TYPE_INT_ARGB)
    for (x in 0 until image.width) {
        if (x != pendingSeam.first().first) throw RuntimeException()
        for (y in 0 until image.height) {
            val seamY = pendingSeam.first().second
            val effectiveY = when {
                y == seamY -> continue
                0 <= y && y < seamY -> y
                seamY < y && y < image.height -> y - 1
                else -> throw RuntimeException()
            }
            newImage.setRGB(x, effectiveY, image.getRGB(x, y))
        }
        pendingSeam.removeFirst()
    }
    return newImage
}

private fun findVerticalMinimumSeam(image: BufferedImage): List<Pair<Int, Int>> {
    val lastX = image.width - 1
    val lastY = image.height - 1
    val minEnergySum: Array<Array<Double>> = Array(image.width) { Array(image.height) { 0.0 } }
    val minimumSeam: MutableList<Pair<Int, Int>> = mutableListOf()
    forEveryPixelTopToBottom(image) { x, y ->
        val energyXY = pixelEnergy(image, x, y)
        minEnergySum[x][y] = energyXY +
                if (y > 0) {
                    val indices = createIndicesFor(x, image.width)
                    indices.minOf { minEnergySum[it][y - 1] }
                } else 0.0 // For first line it's just energy
    }

    // Take min sum on the bottom line and reconstruct the shortest path line by line bottom up
    return (lastY downTo 0).fold(mutableListOf<Pair<Int, Int>>()) { acc, y ->
        when (y) {
            lastY -> {
                val x = minEnergySum.indices.minByOrNull { minEnergySum[it][lastY] }!!
                acc.add(Pair(x, lastY))
                acc
            }

            else -> {
                val indices = createIndicesFor(acc.last().first, image.width)
                val x =
                    indices.minByOrNull { minEnergySum[it][y] }!! // X where min sum in 3 (or 2) pixels on the prev line
                acc.add(Pair(x, y))
                acc
            }
        }
    }.toList()
}

private fun findHorizontalMinimumSeam(image: BufferedImage): List<Pair<Int, Int>> {
    val lastX = image.width - 1
    val lastY = image.height - 1
    val minEnergySum: Array<Array<Double>> = Array(image.width) { Array(image.height) { 0.0 } }
    val minimumSeam: MutableList<Pair<Int, Int>> = mutableListOf()
    forEveryPixelLeftToRight(image) { x, y ->
        val energyXY = pixelEnergy(image, x, y)
        minEnergySum[x][y] = energyXY +
                if (x > 0) {
                    val indices = createIndicesFor(y, image.height)
                    indices.minOf { minEnergySum[x - 1][it] }
                } else 0.0 // For first line it's just energy
    }

    // Take min sum on the bottom line and reconstruct the shortest path line by line bottom up
    return (lastX downTo 0).fold(mutableListOf<Pair<Int, Int>>()) { acc, x ->
        when (x) {
            lastX -> {
                val y = minEnergySum[x].indices.minByOrNull { minEnergySum[x][it] }!!
                acc.add(Pair(x, y))
                acc
            }

            else -> {
                val indices = createIndicesFor(acc.last().second, image.height)
                val y =
                    indices.minByOrNull { minEnergySum[x][it] }!! // X where min sum in 3 (or 2) pixels on the prev line
                acc.add(Pair(x, y))
                acc
            }
        }
    }.toList()
}

private fun createIndicesFor(x: Int, max: Int) =
    listOf(x - 1, x, x + 1).filter { it in 1 until max }

private fun deltaSquare(a: Color, b: Color): Double {
    return (a.red - b.red).toDouble().pow(2.0) +
            (a.green - b.green).toDouble().pow(2.0) +
            (a.blue - b.blue).toDouble().pow(2.0)
}
