package com.classrecord.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.classrecord.app.data.Money
import com.classrecord.app.i18n.AppLanguage
import com.classrecord.app.i18n.LocaleApplier
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "en-rUS")
class EnglishUiScreenshotTest {
    @Test
    fun captureEnglishHomeAndFeeDetailPreviews() {
        LocaleApplier.apply(AppLanguage.EN)
        val context = RuntimeEnvironment.getApplication()
        val artifacts = File("/opt/cursor/artifacts").apply { mkdirs() }

        renderCard(
            file = File(artifacts, "english-home-light.png"),
            background = Color.WHITE,
            lines = listOf(
                "Class 2, Grade 12",
                context.getString(R.string.members_in_class, 12),
                "${context.getString(R.string.home_members)} · ${context.getString(R.string.count_people, 12)}",
                "${context.getString(R.string.home_subgroups)} · ${context.getString(R.string.count_groups, 1)}",
                "${context.getString(R.string.home_ledger)} · ${
                    context.getString(
                        R.string.home_ledger_balance,
                        Money.formatDisplay(context, 41_500L),
                    )
                }",
                "${context.getString(R.string.type_payment)} · ${context.getString(R.string.scope_whole_class)}",
                "Autumn trip fee",
                context.getString(R.string.hint_payment_unfinished, 5),
            ),
        )

        renderCard(
            file = File(artifacts, "english-fee-detail-light.png"),
            background = Color.WHITE,
            lines = listOf(
                "Autumn trip fee",
                context.getString(R.string.progress_paid, 7, 12),
                context.getString(R.string.copy_wechat_reminder),
                context.getString(R.string.record_to_ledger),
                "${context.getString(R.string.student_no_label, "2026003")} · " +
                    context.getString(R.string.amount_due_short, Money.formatDisplay(context, 20_000L)) +
                    " · ${context.getString(R.string.amount_paid_short, context.getString(R.string.em_dash))}",
            ),
        )

        renderCard(
            file = File(artifacts, "english-home-dark.png"),
            background = Color.parseColor("#121212"),
            textColor = Color.WHITE,
            lines = listOf(
                "Class 2, Grade 12",
                context.getString(R.string.members_in_class, 12),
            ),
        )
    }

    private fun renderCard(
        file: File,
        background: Int,
        lines: List<String>,
        textColor: Int = Color.BLACK,
    ) {
        val width = 1080
        val lineHeight = 56
        val height = (lines.size + 2) * lineHeight
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(background)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 34f
        }
        var y = lineHeight.toFloat()
        lines.forEachIndexed { index, line ->
            canvas.drawText(line, 48f, y, if (index == 0) titlePaint else bodyPaint)
            y += lineHeight
        }
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
