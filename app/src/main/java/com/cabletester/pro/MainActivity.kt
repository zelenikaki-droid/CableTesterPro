package com.cabletester.pro


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cabletester.pro.repository.ProfileRepository
import com.cabletester.pro.ui.home.HomeScreen
import com.cabletester.pro.ui.theme.CableTesterProTheme
import com.cabletester.pro.viewmodel.MainViewModel



class MainActivity : ComponentActivity() {


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)


        enableEdgeToEdge()



        val repository =
            ProfileRepository(
                applicationContext
            )



        setContent {


            CableTesterProTheme {


                val viewModel: MainViewModel =
                    viewModel(
                        factory =
                            object :
                                ViewModelProvider.Factory {


                                override fun <T : ViewModel> create(
                                    modelClass: Class<T>
                                ): T {


                                    return MainViewModel(
                                        repository
                                    ) as T
                                }
                            }
                    )


                HomeScreen(
                    viewModel = viewModel
                )

            }
        }
    }
}