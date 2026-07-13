package eu.kanade.presentation.easteregg.aurora

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * «Сигильная панель» — скрытый канал ввода ответа жестом.
 * Сетка 3x3, как графический ключ Android. Пользователь ведёт пальцем
 * путь; при отпускании путь кодируется в список ячеек (1..9) и отдаётся
 * наружу. Код НЕ знает правильный путь — любой жест просто превращается
 * в фразу и проверяется криптографически в AuroraQuest.offer().
 *
 * Никакой индикации «верно/неверно» здесь быть не должно —
 * обратная связь только через общий обработчик AuroraEcho.
 */
@Composable
fun AuroraSigilPad(
    onSigil: (List<Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xE6050B14))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Brutalist tactical frame for high-end (Nolan ritual feel)
            Text(
                text = "[ SIGIL INPUT ]",
                color = Color(0xFFE61919), // hazard red accent
                fontSize = 12.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Text(
                text = "НАЧЕРТИ СИГИЛЬ",
                color = Color(0xFFB8D8FF),
                fontSize = 15.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                letterSpacing = (-0.5).sp,
            )

            val path = remember { mutableStateListOf<Int>() }
            var fingertip by remember { mutableStateOf<Offset?>(null) }
            val view = LocalView.current

            // Project living aurora using new shader (AC6)
            // High-end asset ref: app/src/main/res/drawable/aurora_cinematic/sigil_ref.txt (generated images/7.jpg)
            AuroraSigilProjection(intensity = 0.3f, modifier = Modifier.size(264.dp))
            Canvas(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(264.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { pos ->
                                path.clear()
                                fingertip = pos
                                registerPoint(pos, Size(size.width.toFloat(), size.height.toFloat()), path)
                                if (path.isNotEmpty()) AuroraSensory.node(view)
                            },
                            onDrag = { change, _ ->
                                // БАГФИКС: быстрый жест даёт редкие сэмплы и «перепрыгивал»
                                // промежуточные ячейки (например, центр при вертикали).
                                // Сэмплируем отрезок от прошлой точки до текущей.
                                val sz = Size(size.width.toFloat(), size.height.toFloat())
                                val from = fingertip ?: change.position
                                fingertip = change.position
                                val cellsBefore = path.size
                                val steps = 16
                                for (i in 0..steps) {
                                    val t = i.toFloat() / steps
                                    val pos = Offset(
                                        from.x + (change.position.x - from.x) * t,
                                        from.y + (change.position.y - from.y) * t,
                                    )
                                    registerPoint(pos, sz, path)
                                }
                                // Хаптика: каждый новый узел отзывается лёгким тиком
                                if (path.size > cellsBefore) AuroraSensory.node(view)
                            },
                            onDragEnd = {
                                val result = path.toList()
                                path.clear()
                                fingertip = null
                                if (result.size >= 3) {
                                    AuroraSensory.seal(view)
                                    onSigil(result)
                                }
                            },
                            onDragCancel = {
                                path.clear()
                                fingertip = null
                            },
                        )
                    },
            ) {
                // Линии между выбранными ячейками
                val glow = Color(0xFF64D2FF)
                for (i in 0 until path.size - 1) {
                    drawLine(
                        color = glow.copy(alpha = 0.85f),
                        start = cellCenter(path[i], size),
                        end = cellCenter(path[i + 1], size),
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                // Линия от последней ячейки к пальцу
                val tip = fingertip
                if (tip != null && path.isNotEmpty()) {
                    drawLine(
                        color = glow.copy(alpha = 0.35f),
                        start = cellCenter(path.last(), size),
                        end = tip,
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                // Точки сетки
                for (cell in 1..9) {
                    val center = cellCenter(cell, size)
                    val selected = cell in path
                    if (selected) {
                        drawCircle(
                            color = glow.copy(alpha = 0.25f),
                            radius = 18.dp.toPx(),
                            center = center,
                        )
                    }
                    drawCircle(
                        color = if (selected) glow else Color(0xFF3A5A7A),
                        radius = if (selected) 8.dp.toPx() else 6.dp.toPx(),
                        center = center,
                    )
                }
            }
        }
    }
}

private fun cellCenter(cell: Int, size: Size): Offset {
    val col = (cell - 1) % 3
    val row = (cell - 1) / 3
    val stepX = size.width / 3f
    val stepY = size.height / 3f
    return Offset(stepX * (col + 0.5f), stepY * (row + 0.5f))
}

private fun hitCell(pos: Offset, size: Size): Int? {
    if (size.width <= 0f || size.height <= 0f) return null
    val radius = size.width / 8f
    for (cell in 1..9) {
        if ((pos - cellCenter(cell, size)).getDistance() < radius) return cell
    }
    return null
}

/**
 * Добавляет ячейку под точкой касания. Если между последней и новой
 * ячейкой лежит пропущенная промежуточная — вставляем её тоже
 * (как в графическом ключе Android: 2 -> 8 всегда проходит через 5).
 */
private fun registerPoint(pos: Offset, size: Size, path: MutableList<Int>) {
    val cell = hitCell(pos, size) ?: return
    if (cell in path) return
    val last = path.lastOrNull()
    if (last != null) {
        val bridge = bridgeCell(last, cell)
        if (bridge != null && bridge !in path) path.add(bridge)
    }
    path.add(cell)
}

private fun bridgeCell(a: Int, b: Int): Int? {
    val ar = (a - 1) / 3
    val ac = (a - 1) % 3
    val br = (b - 1) / 3
    val bc = (b - 1) % 3
    if ((ar + br) % 2 != 0 || (ac + bc) % 2 != 0) return null
    val mid = ((ar + br) / 2) * 3 + (ac + bc) / 2 + 1
    return if (mid != a && mid != b) mid else null
}
