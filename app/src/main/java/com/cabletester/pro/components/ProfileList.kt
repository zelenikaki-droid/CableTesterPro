package com.cabletester.pro.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cabletester.pro.model.CableProfile

@Composable
fun ProfileList(

    profiles: List<CableProfile>,

    selectedProfile: CableProfile,

    onProfileSelected: (CableProfile) -> Unit,

    onNewProfile: () -> Unit

) {

    Column(

        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(Color(0xFFE8EAF6))
            .padding(12.dp)

    ) {

        Text(

            text = "Profili",

            style = MaterialTheme.typography.headlineSmall

        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(

            value = "",

            onValueChange = {},

            label = { Text("Iskanje") },

            modifier = Modifier.fillMaxWidth()

        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(

            onClick = onNewProfile,

            modifier = Modifier.fillMaxWidth()

        ) {

            Text("+ Nov profil")

        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(

            verticalArrangement = Arrangement.spacedBy(8.dp)

        ) {

            items(profiles) { profile ->

                val selected = profile.articleCode == selectedProfile.articleCode

                Card(

                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {

                            onProfileSelected(profile)

                        },

                    colors = CardDefaults.cardColors(

                        containerColor =
                            if (selected)
                                Color(0xFFBBDEFB)
                            else
                                Color.White

                    ),

                    shape = RoundedCornerShape(10.dp)

                ) {

                    Column(

                        modifier = Modifier.padding(12.dp)

                    ) {

                        Text(

                            text = profile.articleCode,

                            style = MaterialTheme.typography.titleMedium

                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(

                            text = profile.articleName,

                            style = MaterialTheme.typography.bodyMedium

                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(

                            text =
                                if (profile.hasReference)
                                    "✔ Referenčni vzorec"
                                else
                                    "❌ Brez vzorca",

                            style = MaterialTheme.typography.bodySmall

                        )

                    }

                }

            }

        }

    }

}