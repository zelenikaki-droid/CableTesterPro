package com.cabletester.pro.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cabletester.pro.model.CableProfile

@Composable
fun ProfileDetails(
    profile: CableProfile
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Text(
            text = "Podatki o kablu",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = profile.articleCode,
            onValueChange = {},
            readOnly = true,
            label = { Text("Koda artikla") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = profile.articleName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Ime artikla") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = profile.description,
            onValueChange = {},
            readOnly = true,
            label = { Text("Opis") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text("Fotografija")

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .border(
                    1.dp,
                    Color.Gray,
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("Fotografija še ni dodana")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Datum izdelave: ${profile.created}")

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            if (profile.lastTest == null)
                "Zadnji test: Ni izveden"
            else
                "Zadnji test: ${profile.lastTest}"
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            if (profile.hasReference)
                "Referenčni vzorec: ✔ Posnet"
            else
                "Referenčni vzorec: ❌ Ni posnet"
        )

        Spacer(modifier = Modifier.height(30.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Button(onClick = {}) {
                Text("SELF LEARN")
            }

            Button(onClick = {}) {
                Text("TEST")
            }

            Button(onClick = {}) {
                Text("SHRANI")
            }

        }

    }

}