package com.sloth.registerapp.presentation.facedetection.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.google.mlkit.vision.face.Face

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = resolveThemeColor(com.google.android.material.R.attr.colorPrimary, Color.GREEN)
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = resolveThemeColor(com.google.android.material.R.attr.colorPrimary, Color.GREEN)
        textSize = 48f
        style = Paint.Style.FILL
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(4f, 2f, 2f, resolveThemeColor(android.R.attr.textColorPrimary, Color.BLACK))
    }

    private var faces: List<Face>? = null
    private var faceNames: Map<Int, String> = emptyMap()
    private var imageWidth = 0
    private var imageHeight = 0
    private var isFrontCamera = true

    private var rotation: Int = 0

    private fun resolveThemeColor(attr: Int, fallback: Int): Int {
        val typedValue = TypedValue()
        return if (context.theme.resolveAttribute(attr, typedValue, true)) {
            typedValue.data
        } else {
            fallback
        }
    }

    fun setFaces(
        faces: List<Face>,
        names: Map<Int, String>,
        imageWidth: Int,
        imageHeight: Int,
        isFrontCamera: Boolean,
        rotation: Int // em graus, geralmente 0, 90, 180, 270
    ) {
        this.faces = faces
        this.faceNames = names
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        this.isFrontCamera = isFrontCamera
        this.rotation = rotation
        invalidate()
    }

    fun clear() {
        faces = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val facesList = faces ?: return
        if (imageWidth == 0 || imageHeight == 0 || width == 0 || height == 0) return

        facesList.forEachIndexed { index, face ->
            val boundingBox = face.boundingBox

            // Converte coordenadas da imagem para coordenadas da view
            val rect = translateRect(
                RectF(boundingBox),
                imageWidth,
                imageHeight,
                width,
                height,
                isFrontCamera,
                rotation
            )

            // Desenha retângulo
            canvas.drawRect(rect, paint)

            // Desenha ID do rosto
            val name = faceNames[index] ?: "Desconhecido(a) #${index + 1}"
            canvas.drawText(name, rect.left, rect.top - 15, textPaint)
        }
    }

    /**
     * Traduz coordenadas da imagem da câmera para a view overlay
     */
    private fun translateRect(
        rect: RectF,
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Int,
        viewHeight: Int,
        isFrontCamera: Boolean,
        rotation: Int
    ): RectF {
        var newRect = RectF(rect)

        // Ajusta para portrait
        if (rotation == 90 || rotation == 270) {
            // Gira retângulo de landscape para portrait
            newRect = RectF(
                rect.top,
                imageWidth - rect.right,
                rect.bottom,
                imageWidth - rect.left
            )
            // Também swap imageWidth/imageHeight
        }

        val scaleX = viewWidth.toFloat() / (if (rotation == 90 || rotation == 270) imageHeight else imageWidth)
        val scaleY = viewHeight.toFloat() / (if (rotation == 90 || rotation == 270) imageWidth else imageHeight)
        val scale = scaleX.coerceAtLeast(scaleY)

        val scaledImageWidth = (if (rotation == 90 || rotation == 270) imageHeight else imageWidth) * scale
        val scaledImageHeight = (if (rotation == 90 || rotation == 270) imageWidth else imageHeight) * scale
        val offsetX = (viewWidth - scaledImageWidth) / 2
        val offsetY = (viewHeight - scaledImageHeight) / 2

        val left = newRect.left * scale + offsetX
        val top = newRect.top * scale + offsetY
        val right = newRect.right * scale + offsetX
        val bottom = newRect.bottom * scale + offsetY

        // Espelha horizontalmente se for câmera frontal
        val finalLeft = if (isFrontCamera) viewWidth - right else left
        val finalRight = if (isFrontCamera) viewWidth - left else right

        return RectF(finalLeft, top, finalRight, bottom)
    }

}
