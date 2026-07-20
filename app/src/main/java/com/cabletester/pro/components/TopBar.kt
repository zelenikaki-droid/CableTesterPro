package com.cabletester.pro.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TopBar(

    bluetoothConnected: Boolean = false

) {

    Row(

        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement = Arrangement.SpaceBetween,

        verticalAlignment = Alignment.CenterVertically

    ) {

        Text(

            text = "Cable Tester Pro",

            style = MaterialTheme.typography.headlineMedium

        )

        Text(

            text = if (bluetoothConnected)
                "🟢 HC-05"
            else
                "🔴 HC-05",

            color = if (bluetoothConnected)
                Color(0xFF2E7D32)
            else
                Color.Red
        )
    }

    Spacer(modifier = Modifier.height(20.dp))
}