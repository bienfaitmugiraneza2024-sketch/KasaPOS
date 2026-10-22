package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Store
import com.example.ui.state.KasaViewModel

@Composable
fun SwitchStaffDialog(
    viewModel: KasaViewModel,
    onDismiss: () -> Unit,
    onRegisterClick: () -> Unit = {}
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Changer d'utilisateur / Déverrouiller") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Saisissez votre code PIN personnel à 4 chiffres (ou '0000' pour le compte Administrateur / Gérant).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = {
                        if (it.length <= 4) enteredPin = it
                    },
                    label = { Text("Code PIN (4 chiffres)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("pin_code_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                TextButton(
                    onClick = onRegisterClick,
                    modifier = Modifier.fillMaxWidth().testTag("open_register_code_btn")
                ) {
                    Text("Nouveau collaborateur ? S'inscrire avec un code d'invitation", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val success = viewModel.switchStaffWithPin(enteredPin)
                    if (success) {
                        onDismiss()
                    } else {
                        errorMessage = "Code PIN incorrect"
                    }
                },
                modifier = Modifier.testTag("submit_pin_button")
            ) {
                Text("Déverrouiller")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun InviteEmployeeDialog(
    viewModel: KasaViewModel,
    onDismiss: () -> Unit
) {
    val stores by viewModel.stores.collectAsState()
    var selectedStore by remember { mutableStateOf(stores.firstOrNull()) }
    var role by remember { mutableStateOf("CASHIER") }
    var generatedCode by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inviter un employé") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (generatedCode == null) {
                    Text(
                        text = "Sélectionnez le magasin attribué à ce collaborateur pour générer un code d'invitation unique :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text("Magasin affecté :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    LazyColumn(modifier = Modifier.height(140.dp)) {
                        items(stores) { store ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedStore = store }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedStore?.id == store.id,
                                    onClick = { selectedStore = store }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(store.name, fontWeight = FontWeight.SemiBold)
                                    if (store.address.isNotBlank()) {
                                        Text(store.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Code generated view with WhatsApp sharing
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Code d'accès unique généré :",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = generatedCode ?: "",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Magasin : ${selectedStore?.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val msg = "Bonjour ! Voici votre code d'accès pour rejoindre le magasin '${selectedStore?.name}' sur l'application KasaPOS : *${generatedCode}*.\nOuvrez l'application, cliquez sur 'Rejoindre avec un code d'invitation' et créez votre mot de passe."
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, msg)
                            }
                            context.startActivity(Intent.createChooser(intent, "Partager le code d'accès"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Partager via WhatsApp / SMS")
                    }
                }
            }
        },
        confirmButton = {
            if (generatedCode == null) {
                Button(
                    onClick = {
                        val store = selectedStore ?: stores.firstOrNull()
                        if (store != null) {
                            viewModel.generateInviteCode(store, role) { code ->
                                generatedCode = code
                            }
                        }
                    },
                    modifier = Modifier.testTag("generate_code_btn")
                ) {
                    Text("Générer le code")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("Terminé")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (generatedCode == null) "Annuler" else "Fermer")
            }
        }
    )
}

@Composable
fun RegisterStaffWithCodeDialog(
    viewModel: KasaViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var inviteCodeInput by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var pinCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inscription Employé") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Saisissez le code d'accès reçu de votre employeur pour activer votre compte vendeur :",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = inviteCodeInput,
                    onValueChange = { inviteCodeInput = it.uppercase() },
                    label = { Text("Code d'accès (ex: KASA-1234) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_invite_code")
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Votre nom complet *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_staff_name")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Numéro de téléphone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pinCode,
                    onValueChange = {
                        if (it.length <= 4) pinCode = it
                    },
                    label = { Text("Définir votre code PIN (4 chiffres) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_staff_pin")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (inviteCodeInput.isNotBlank() && fullName.isNotBlank() && pinCode.length == 4) {
                        viewModel.registerStaffWithCode(
                            code = inviteCodeInput,
                            name = fullName,
                            phone = phone,
                            pin = pinCode,
                            onSuccess = onSuccess
                        )
                    } else {
                        errorMessage = "Veuillez remplir tous les champs obligatoires (PIN à 4 chiffres)."
                    }
                },
                modifier = Modifier.testTag("submit_staff_registration")
            ) {
                Text("Valider l'inscription")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
