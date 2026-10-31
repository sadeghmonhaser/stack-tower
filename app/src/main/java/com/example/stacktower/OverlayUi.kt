package com.example.stacktower

import android.content.Context
import android.os.Build
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Shared overlay design system (colors/dimens from resources).
 */
class OverlayUi(private val context: Context) {

    private val density = context.resources.displayMetrics.density

    fun dp(value: Int): Int = (value * density + 0.5f).toInt()

    fun color(resId: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getColor(resId)
        } else {
            @Suppress("DEPRECATION")
            context.resources.getColor(resId)
        }

    fun dimenPx(resId: Int): Int = context.resources.getDimensionPixelSize(resId)

    fun dimenFloat(resId: Int): Float = context.resources.getDimension(resId)

    fun cardWidthPx(): Int =
        (context.resources.displayMetrics.widthPixels * context.resources.getFraction(
            R.fraction.card_width_fraction, 1, 1
        )).toInt()

    fun overlayMaxHeightPx(): Int =
        (context.resources.displayMetrics.heightPixels * context.resources.getFraction(
            R.fraction.overlay_max_height_fraction, 1, 1
        )).toInt()

    fun createCardLayout(): LinearLayout {
        val pad = dimenPx(R.dimen.card_padding)
        val radius = dimenFloat(R.dimen.radius_card)
        val stroke = dimenPx(R.dimen.stroke_card)
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                setColor(color(R.color.surface_card))
                cornerRadius = radius
                setStroke(stroke, color(R.color.surface_card_stroke))
            }
        }
    }

    fun createHeaderTextView(title: CharSequence): TextView =
        TextView(context).apply {
            text = title
            setTextColor(color(R.color.text_primary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.text_title)
            )
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dimenPx(R.dimen.space_lg))
        }

    fun createBodyText(): TextView =
        TextView(context).apply {
            setTextColor(color(R.color.text_secondary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.text_body)
            )
            gravity = Gravity.CENTER
            setPadding(
                dimenPx(R.dimen.space_sm),
                dimenPx(R.dimen.space_sm),
                dimenPx(R.dimen.space_sm),
                dimenPx(R.dimen.space_md)
            )
        }

    fun createMutedBodyText(): TextView =
        createBodyText().apply {
            setTextColor(color(R.color.text_muted))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.text_caption)
            )
        }

    fun createPrimaryButton(text: CharSequence, onClick: () -> Unit): Button =
        createFilledButton(
            text,
            R.color.accent_primary,
            R.color.accent_primary_dark,
            onClick
        )

    fun createSecondaryButton(text: CharSequence, onClick: () -> Unit): Button =
        createFilledButton(
            text,
            R.color.button_secondary,
            R.color.button_secondary_stroke,
            onClick
        )

    fun createCardButtonPrimary(text: CharSequence, onClick: () -> Unit): Button =
        createPrimaryButton(text, onClick).withCardLayoutParams()

    fun createCardButtonSecondary(text: CharSequence, onClick: () -> Unit): Button =
        createSecondaryButton(text, onClick).withCardLayoutParams()

    fun createCardButtonSecondaryCompact(text: CharSequence, onClick: () -> Unit): Button =
        createSecondaryButton(text, onClick).withCardLayoutParams(compact = true)

    fun createHeroTitle(title: CharSequence): TextView =
        TextView(context).apply {
            text = title
            setTextColor(color(R.color.text_primary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.text_display_lg)
            )
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dimenPx(R.dimen.space_sm))
        }

    fun createSubtitleText(): TextView =
        TextView(context).apply {
            setTextColor(color(R.color.text_secondary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.text_body)
            )
            gravity = Gravity.CENTER
            setPadding(
                dimenPx(R.dimen.space_sm),
                0,
                dimenPx(R.dimen.space_sm),
                dimenPx(R.dimen.space_md)
            )
        }

    /** Centered chip row; second value is the bold stat [TextView]. */
    fun createStatChipRow(label: CharSequence): Pair<LinearLayout, TextView> {
        val valueView = TextView(context).apply {
            setTextColor(color(R.color.text_primary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.text_display_sm)
            )
            setTypeface(typeface, Typeface.BOLD)
        }
        val labelView = TextView(context).apply {
            text = label
            setTextColor(color(R.color.text_muted))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.text_caption)
            )
            setPadding(0, 0, dimenPx(R.dimen.space_sm), 0)
        }
        val chip = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dimenPx(R.dimen.space_md), dimenPx(R.dimen.space_sm), dimenPx(R.dimen.space_md), dimenPx(R.dimen.space_sm))
            background = GradientDrawable().apply {
                setColor(adjustAlpha(color(R.color.accent_primary), 0.14f))
                cornerRadius = dimenFloat(R.dimen.radius_chip)
                setStroke(dimenPx(R.dimen.stroke_button), adjustAlpha(color(R.color.accent_primary), 0.45f))
            }
            addView(labelView)
            addView(valueView)
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(chip, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dimenPx(R.dimen.space_md) })
        }
        return row to valueView
    }

    fun createSectionLabel(text: CharSequence): TextView =
        TextView(context).apply {
            this.text = text
            setTextColor(color(R.color.text_muted))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.text_caption)
            )
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(0, dimenPx(R.dimen.space_md), 0, dimenPx(R.dimen.space_xs))
        }

    fun createSubtleDivider(): View {
        val line = android.view.View(context)
        line.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dimenPx(R.dimen.stroke_card)
        ).apply {
            topMargin = dimenPx(R.dimen.space_sm)
            bottomMargin = dimenPx(R.dimen.space_sm)
        }
        line.setBackgroundColor(color(R.color.divider_subtle))
        return line
    }

    fun createScoreHeroTextView(): TextView =
        TextView(context).apply {
            setTextColor(color(R.color.text_primary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.text_score_hero)
            )
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dimenPx(R.dimen.space_xs), 0, dimenPx(R.dimen.space_sm))
        }

    fun createCaptionLabel(text: CharSequence): TextView =
        TextView(context).apply {
            this.text = text
            setTextColor(color(R.color.text_muted))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.text_caption)
            )
            gravity = Gravity.CENTER
            setPadding(0, dimenPx(R.dimen.space_sm), 0, 0)
        }

    fun createInsetPanel(): LinearLayout {
        val pad = dimenPx(R.dimen.space_md)
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                setColor(color(R.color.surface_inset))
                cornerRadius = dimenFloat(R.dimen.radius_chip)
                setStroke(dimenPx(R.dimen.stroke_card), color(R.color.surface_card_stroke))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dimenPx(R.dimen.space_lg)
            }
        }
    }

    fun createChromeButton(text: CharSequence, contentDescription: CharSequence, onClick: () -> Unit): Button =
        createFilledButton(
            text,
            R.color.chrome_pause,
            R.color.chrome_pause_stroke,
            onClick
        ).apply {
            this.contentDescription = contentDescription
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.text_caption)
            )
        }

    fun styledInputBackground(): GradientDrawable =
        GradientDrawable().apply {
            setColor(color(R.color.input_field_bg))
            cornerRadius = dimenFloat(R.dimen.radius_input)
            setStroke(dimenPx(R.dimen.stroke_button), color(R.color.surface_card_stroke))
        }

    private fun createFilledButton(
        textStr: CharSequence,
        fillColorRes: Int,
        strokeColorRes: Int,
        onClick: () -> Unit
    ): Button {
        val radius = dimenFloat(R.dimen.radius_button)
        val stroke = dimenPx(R.dimen.stroke_button)
        val normal = GradientDrawable().apply {
            setColor(color(fillColorRes))
            cornerRadius = radius
            setStroke(stroke, color(strokeColorRes))
        }
        val pressed = GradientDrawable().apply {
            setColor(adjustAlpha(color(fillColorRes), 0.82f))
            cornerRadius = radius
            setStroke(stroke, color(strokeColorRes))
        }
        val bg = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), pressed)
            addState(intArrayOf(), normal)
        }
        return Button(context).apply {
            text = textStr
            setTextColor(color(R.color.text_primary))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                context.resources.getDimension(R.dimen.text_body)
            )
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dimenPx(R.dimen.space_md), dp(10), dimenPx(R.dimen.space_md), dp(10))
            minHeight = dimenPx(R.dimen.button_min_height)
            isAllCaps = false
            background = bg
            setOnClickListener { onClick() }
        }
    }

    private fun Button.withCardLayoutParams(compact: Boolean = false): Button = apply {
        val margin = dimenPx(
            if (compact) R.dimen.button_vertical_margin_compact else R.dimen.button_vertical_margin
        )
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = margin
            bottomMargin = margin
        }
    }

    private fun adjustAlpha(colorInt: Int, factor: Float): Int {
        val a = (Color.alpha(colorInt) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(colorInt), Color.green(colorInt), Color.blue(colorInt))
    }
}
