package com.cabletester.pro.ui.home

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.cabletester.pro.components.ProfileDetails
import com.cabletester.pro.components.ProfileList
import com.cabletester.pro.model.CableProfile

@Composable
fun HomeScreen() {

    val profiles = listOf(

        CableProfile(
            articleCode = "0305122",
            articleName = "Servo kabel",
            description = "Servo motor Siemens",
            created = "19.07.2026",
            hasReference = true
        ),

        CableProfile(
            articleCode = "88436",
            articleName = "Motor kabel",
            description = "Motor 3-fazni",
            created = "19.07.2026"
        ),

        CableProfile(
            articleCode = "93420",
            articleName = "Signalni kabel",
            description = "Signalni kabel PLC",
            created = "19.07.2026"
        )

    )

    var selectedProfile by remember {
        mutableStateOf(profiles.first())
    }

    Row(
        modifier = Modifier.fillMaxSize()
    ) {

        ProfileList(

            profiles = profiles,

            selectedProfile = selectedProfile,

            onProfileSelected = { profile ->

                selectedProfile = profile

            },

            onNewProfile = {

                // To bomo naredili v naslednjem koraku

            }

        )

        ProfileDetails(

            profile = selectedProfile

        )

    }

}