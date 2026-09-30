package com.classrecord.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.classrecord.app.data.Money
import com.classrecord.app.i18n.AppLanguage
import com.classrecord.app.i18n.LocaleApplier
import org.junit.Assume.assumeTrue
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
        val context = RuntimeEnvironment.getApplication()
        LocaleApplier.apply(context, AppLanguage.EN, allowClearToSystem = true)
        val artifacts = resolveArtifactDir(context)
        assumeTrue("Artifact dir not writable: $artifacts", artifacts.mkdirs() || artifacts.isDirectory)

        renderCard(
            file = File(artifacts, "english-home-light.png"),
            background = Color.WHITE,
            lines = listOf(
                "Weekend Badminton Club",
                context.resources.getQuantityString(R.plurals.members_in_class, 12, 12),
                "${context.getString(R.string.home_members)} · ${
                    context.resources.getQuantityString(R.plurals.count_people, 12, 12)
                }",
                "${context.getString(R.string.home_subgroups)} · ${
                    context.resources.getQuantityString(R.plurals.count_groups, 1, 1)
                }",
                "${context.getString(R.string.home_ledger)} · ${
                    context.getString(
                        R.string.home_ledger_balance,
                        Money.formatDisplay(context, 41_500L),
                    )
                }",
                "${context.getString(R.string.type_payment)} · ${context.getString(R.string.scope_whole_class)}",
                "Autumn outing fee",
                context.getString(R.string.hint_payment_unfinished, 5),
            ),
        )

        renderCard(
            file = File(artifacts, "english-fee-detail-light.png"),
            background = Color.WHITE,
            lines = listOf(
                "Autumn outing fee",
                context.getString(R.string.progress_paid, 7, 12),
                context.getString(R.string.copy_wechat_reminder),
                context.getString(R.string.record_to_ledger),
                "${context.getString(R.string.student_no_label, "2026003")} · " +
                    context.getString(R.string.amount_due_short, Money.formatDisplay(context, 20_000L)),
            ),
        )

        renderCard(
            file = File(artifacts, "english-home-dark.png"),
            background = Color.parseColor("#121212"),
            textColor = Color.WHITE,
            lines = listOf(
                "Weekend Badminton Club",
                context.resources.getQuantityString(R.plurals.members_in_class, 12, 12),
            ),
        )
    }

    private fun resolveArtifactDir(context: android.content.Context): File {
        val env = System.getenv("CURSOR_ARTIFACTS")
        if (!env.isNullOrBlank()) {
            return File(env)
        }
        return File(context.cacheDir, "test-screenshots")
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
