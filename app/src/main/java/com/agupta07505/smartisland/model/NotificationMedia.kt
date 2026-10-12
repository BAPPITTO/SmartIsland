/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.model

import android.graphics.Bitmap

/**
 * One picture, sticker or animated sticker a notification carries (what the shade shows as the
 * message's image). [still] is always set; [animatedBytes] is the original file, kept only for
 * animated images (WebP / GIF stickers) so the island can play them. A plain class on purpose:
 * it holds a bitmap and a byte array, so equality is by identity.
 */
class NotificationMedia(
    val still: Bitmap,
    val animatedBytes: ByteArray? = null,
    val isSticker: Boolean = false
) {
}
