package io.github.composegears.valkyrie.psi.imagevector.parser

import io.github.composegears.valkyrie.extensions.safeAs
import io.github.composegears.valkyrie.ir.IrColor
import io.github.composegears.valkyrie.ir.IrFill
import io.github.composegears.valkyrie.ir.IrImageVector
import io.github.composegears.valkyrie.ir.IrStrokeLineJoin
import io.github.composegears.valkyrie.ir.IrVectorNode
import io.github.composegears.valkyrie.ir.IrVectorNode.IrPath
import io.github.composegears.valkyrie.psi.extension.childOfType
import io.github.composegears.valkyrie.psi.extension.childrenOfType
import io.github.composegears.valkyrie.psi.imagevector.common.extractPathFillType
import io.github.composegears.valkyrie.psi.imagevector.common.parsePath
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

internal object MaterialImageVectorPsiParser {

    fun parse(ktFile: KtFile): IrImageVector? {
        val property = ktFile.childOfType<KtProperty>() ?: return null
        val blockBody = property.getter?.bodyBlockExpression ?: return null

        val materialIconCall = blockBody.childrenOfType<KtCallExpression>().firstOrNull {
            it.calleeExpression?.text == "materialIcon"
        } ?: return null

        return IrImageVector(
            name = materialIconCall.extractIconName(),
            defaultWidth = 24f,
            defaultHeight = 24f,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = materialIconCall.extractAutoMirror(),
            nodes = blockBody.parseMaterialPath(),
        )
    }

    private fun KtCallExpression.extractIconName(): String {
        val nameArgument = valueArguments.find { arg ->
            arg?.getArgumentName()?.asName?.identifier == "name"
        }

        return nameArgument?.getArgumentExpression().safeAs<KtStringTemplateExpression>()
            ?.entries
            ?.firstOrNull()
            ?.text.orEmpty()
    }

    private fun KtCallExpression.extractAutoMirror(): Boolean {
        val autoMirrorArgument = valueArguments.find { arg ->
            arg?.getArgumentName()?.asName?.identifier == "autoMirror"
        }

        return autoMirrorArgument?.getArgumentExpression()?.text?.toBoolean() ?: false
    }

    private fun KtBlockExpression.parseMaterialPath(): List<IrVectorNode> {
        val materialPathCall = childrenOfType<KtCallExpression>().firstOrNull {
            it.calleeExpression?.text == "materialPath"
        } ?: return emptyList()

        val pathLambda = materialPathCall.lambdaArguments.firstOrNull()?.getLambdaExpression()
        val pathBody = pathLambda?.bodyExpression ?: return emptyList()

        return listOf(
            IrPath(
                fill = IrFill.Color(IrColor("#FF000000")),
                fillAlpha = materialPathCall.extractFloat("fillAlpha", 1f),
                strokeAlpha = materialPathCall.extractFloat("strokeAlpha", 1f),
                strokeLineWidth = 1f,
                strokeLineJoin = IrStrokeLineJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = materialPathCall.extractPathFillType(),
                paths = pathBody.parsePath(),
            ),
        )
    }

    private fun KtCallExpression.extractFloat(argName: String, defaultValue: Float): Float {
        val argument = valueArguments.find { arg ->
            arg?.getArgumentName()?.asName?.identifier == argName
        }

        return argument?.getArgumentExpression()?.text?.toFloatOrNull() ?: defaultValue
    }
}
