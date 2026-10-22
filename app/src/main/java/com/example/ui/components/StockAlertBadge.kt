package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.theme.AlertWarning
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen

@Composable
fun StockAlertBadge(
    product: Product,
    modifier: Modifier = Modifier
) {
    val (label, bg, fg) = when {
        product.isOutOfStock -> Triple(
            "Rupture",
            LossRed.copy(alpha = 0.15f),
            LossRed
        )
        product.isLowStock -> Triple(
            "Stock faible (${product.stockQuantity.toInt()} ${product.unit})",
            AlertWarning.copy(alpha = 0.15f),
            AlertWarning
        )
        else -> Triple(
            "En stock (${product.stockQuantity.toInt()})",
            ProfitGreen.copy(alpha = 0.12f),
            ProfitGreen
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = fg
            )
        }
    }
}
