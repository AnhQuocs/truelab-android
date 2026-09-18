package dev.anhquocs.truelab.core.ui.utils

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

val Typography.s10: TextStyle get() = labelLarge.copy(fontSize = 10.sp, lineHeight = 14.sp)
val Typography.s11: TextStyle get() = labelLarge.copy(fontSize = 11.sp, lineHeight = 15.sp)
val Typography.s12: TextStyle get() = labelLarge.copy(fontSize = 12.sp, lineHeight = 16.sp)
val Typography.s13: TextStyle get() = bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp)
val Typography.s14: TextStyle get() = bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp)
val Typography.s15: TextStyle get() = bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp)
val Typography.s16: TextStyle get() = titleMedium.copy(fontSize = 16.sp, lineHeight = 24.sp)
val Typography.s18: TextStyle get() = titleLarge.copy(fontSize = 18.sp, lineHeight = 26.sp)
val Typography.s20: TextStyle get() = headlineMedium.copy(fontSize = 20.sp, lineHeight = 28.sp)
val Typography.s22: TextStyle get() = headlineLarge.copy(fontSize = 22.sp, lineHeight = 30.sp)
val Typography.s24: TextStyle get() = displaySmall.copy(fontSize = 24.sp, lineHeight = 32.sp)
val Typography.s28: TextStyle get() = displayMedium.copy(fontSize = 28.sp, lineHeight = 36.sp)
val Typography.s32: TextStyle get() = displayLarge.copy(fontSize = 32.sp, lineHeight = 40.sp)

// --- Font Weight ---
fun TextStyle.bold() = this.copy(fontWeight = FontWeight.Bold)
fun TextStyle.semiBold() = this.copy(fontWeight = FontWeight.SemiBold)
fun TextStyle.medium() = this.copy(fontWeight = FontWeight.Medium)
fun TextStyle.normal() = this.copy(fontWeight = FontWeight.Normal)
fun TextStyle.light() = this.copy(fontWeight = FontWeight.Light)

// --- Style & Decoration ---
fun TextStyle.italic() = this.copy(fontStyle = FontStyle.Italic)
fun TextStyle.underline() = this.copy(textDecoration = TextDecoration.Underline)
fun TextStyle.lineThrough() = this.copy(textDecoration = TextDecoration.LineThrough)