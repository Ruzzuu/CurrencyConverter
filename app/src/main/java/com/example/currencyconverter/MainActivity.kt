package com.example.currencyconverter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke // Keep if used, but not for the OutlinedButton selector styling
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
// import androidx.compose.foundation.border // Not needed for the OutputPane if it's an OutlinedTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // For DropdownMenu custom colors
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.currencyconverter.ui.theme.CurrencyConverterTheme
import java.text.NumberFormat
import java.util.Locale

enum class ConversionDirection {
    USD_TO_IDR,
    IDR_TO_USD
}
data class SelectableCurrency(val code: String, val displayName: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CurrencyConverterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MinimalCenteredConverterScreenWithLogo()
                }
            }
        }
    }
}

@Composable
fun MinimalCenteredConverterScreenWithLogo() {
    val exchangeRateUsdToIdr = 16000.0
    var amountInput by remember { mutableStateOf("") }
    var convertedAmount by remember { mutableStateOf("") }

    val availableCurrencies = listOf(
        SelectableCurrency("USD", "US Dollar"),
        SelectableCurrency("IDR", "Indonesian Rupiah"),
        SelectableCurrency("EUR", "Euro"),
        SelectableCurrency("JPY", "Japanese Yen")
    )

    var sourceCurrency by remember { mutableStateOf(availableCurrencies[0]) }
    var targetCurrency by remember { mutableStateOf(availableCurrencies[1]) }

    val ratesToUsd = mapOf(
        "USD" to 1.0,
        "IDR" to 1 / 16000.0,
        "EUR" to 1 / 0.92,
        "JPY" to 1 / 155.0
    )
    val ratesFromUsd = mapOf(
        "USD" to 1.0,
        "IDR" to 16000.0,
        "EUR" to 0.92,
        "JPY" to 155.0
    )

    val currentDirection = remember(sourceCurrency, targetCurrency) {
        // Simplified logic for direction based on your previous example
        // This might need adjustment if you add more than USD/IDR direct rates
        if (sourceCurrency.code == "USD" && targetCurrency.code == "IDR") {
            ConversionDirection.USD_TO_IDR
        } else if (sourceCurrency.code == "IDR" && targetCurrency.code == "USD") {
            ConversionDirection.IDR_TO_USD
        } else {
            // Fallback or more complex logic if needed for other pairs
            // For now, let's assume default USD_TO_IDR if not IDR_TO_USD
            // This part might need more robust handling for EUR/JPY etc.
            if (sourceCurrency.code == "USD") ConversionDirection.USD_TO_IDR
            else ConversionDirection.IDR_TO_USD // Example, might not be correct for all pairs
        }
    }

    val idrFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
        maximumFractionDigits = 0
    }
    val usdFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 2
    }

    fun performConversion() {
        if (amountInput.isNotBlank()) {
            try {
                val inputValue = amountInput.toDouble()
                val valueInUsd = inputValue * (ratesToUsd[sourceCurrency.code] ?: 1.0)
                val result = valueInUsd * (ratesFromUsd[targetCurrency.code] ?: 1.0)
                convertedAmount = when (targetCurrency.code) {
                    "IDR" -> idrFormat.format(result)
                    "USD" -> usdFormat.format(result)
                    else -> "%.2f".format(result)
                }
            } catch (e: NumberFormatException) {
                convertedAmount = "Invalid input"
            }
        } else {
            convertedAmount = ""
        }
    }

    // Re-calculate when amount or currencies change
    LaunchedEffect(amountInput, sourceCurrency, targetCurrency) {
        performConversion()
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.currency_exchange_logo), // Ensure this is correct
                contentDescription = "Currency Exchange Logo",
                modifier = Modifier
                    .height(100.dp)
                    .padding(bottom = 24.dp)
            )

            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .wrapContentHeight()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround // Or Arrangement.spacedBy(8.dp)
                ) {
                    MinimalCurrencySelector(
                        modifier = Modifier.weight(2f),
                        selectedCurrency = sourceCurrency,
                        availableCurrencies = availableCurrencies,
                        onCurrencySelected = { selected ->
                            if (selected.code == targetCurrency.code) { // Prevent selecting same currency
                                targetCurrency = sourceCurrency // Swap if trying to select target as source
                            }
                            sourceCurrency = selected
                        }
                    )

                    IconButton(
                        onClick = {
                            val tempSource = sourceCurrency
                            sourceCurrency = targetCurrency
                            targetCurrency = tempSource

                            // Smart swap of amount field
                            val currentInputText = amountInput
                            val currentOutputText = convertedAmount

                            if (currentOutputText.isNotBlank() && currentOutputText != "Invalid input") {
                                try {
                                    // Attempt to parse based on the NEW source currency (which was the old target)
                                    val parsedValue = when (sourceCurrency.code) {
                                        "IDR" -> idrFormat.parse(currentOutputText)?.toDouble()
                                        "USD" -> usdFormat.parse(currentOutputText)?.toDouble()
                                        // Add other cases if more specific parsing is needed for EUR, JPY
                                        else -> currentOutputText.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
                                    }
                                    amountInput = parsedValue?.let {
                                        // Format back to a simple string for the input field, avoid currency symbols
                                        if (sourceCurrency.code == "IDR") "%.0f".format(it) else "%.2f".format(it)
                                    } ?: ""
                                } catch (e: Exception) {
                                    amountInput = "" // Fallback on parsing error
                                }
                            } else {
                                amountInput = "" // Clear input if output was blank
                            }
                            // Conversion will be re-triggered by LaunchedEffect
                        },
                        modifier = Modifier.weight(1f) // Adjust weight as needed
                    ) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = "Swap currencies",
                            tint = MaterialTheme.colorScheme.primary)
                    }

                    MinimalCurrencySelector(
                        modifier = Modifier.weight(2f),
                        selectedCurrency = targetCurrency,
                        availableCurrencies = availableCurrencies,
                        onCurrencySelected = { selected ->
                            if (selected.code == sourceCurrency.code) { // Prevent selecting same currency
                                sourceCurrency = targetCurrency // Swap if trying to select source as target
                            }
                            targetCurrency = selected
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    // .height(IntrinsicSize.Min), // Let TextFields determine their height
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top // Align based on the top of the TextFields
                ) {
                    MinimalInputPane(
                        modifier = Modifier.weight(1f),
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        currencyCode = sourceCurrency.code
                    )

                    MinimalOutputPane(
                        modifier = Modifier.weight(1f),
                        text = if(amountInput.isBlank()) "" else convertedAmount, // Show placeholder or result
                        currencyCode = targetCurrency.code
                    )
                }
            }
        }
    }
}


@Composable
fun MinimalCurrencySelector(
    modifier: Modifier = Modifier,
    selectedCurrency: SelectableCurrency,
    availableCurrencies: List<SelectableCurrency>,
    onCurrencySelected: (SelectableCurrency) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // Custom colors for the DropdownMenu background and its text items
    val dropdownBackgroundColor = Color(0xFF008cff) // Your specified blue for dropdown
    val dropdownFontColor = Color.White          // White font for dropdown items

    Box(modifier = modifier) {
        // Standard OutlinedButton, no custom border or text color here
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            // shape = MaterialTheme.shapes.extraLarge, // Or your preferred shape
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp) // Default padding
        ) {
            Text(
                selectedCurrency.code,
                fontWeight = FontWeight.Medium, // Default font weight
                modifier = Modifier.weight(1f)
                // Text color will be default from Theme (e.g., MaterialTheme.colorScheme.primary)
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = "Select ${selectedCurrency.displayName}"
                // Icon tint will be default from Theme
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .widthIn(min = 150.dp)
                .background(dropdownBackgroundColor) // APPLY CUSTOM BACKGROUND COLOR to Dropdown
        ) {
            availableCurrencies.forEach { currency ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "${currency.displayName} (${currency.code})",
                            color = dropdownFontColor // APPLY CUSTOM FONT COLOR to Dropdown items
                        )
                    },
                    onClick = {
                        onCurrencySelected(currency)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun MinimalInputPane(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    currencyCode: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(currencyCode) },
        placeholder = { Text("0.00") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true, // Consistent with output
        shape = MaterialTheme.shapes.medium, // Consistent shape
        textStyle = TextStyle(fontSize = 18.sp)
    )
}

@Composable
fun MinimalOutputPane(
    modifier: Modifier = Modifier,
    text: String,
    currencyCode: String
) {
    OutlinedTextField(
        value = if (text.isBlank()) "" else text,
        onValueChange = { /* Read-only */ },
        modifier = modifier,
        label = { Text(currencyCode) },
        placeholder = { Text("---") },
        readOnly = true,
        shape = MaterialTheme.shapes.medium, // Consistent shape
        textStyle = TextStyle(fontSize = 18.sp, textAlign = TextAlign.Start),
        colors = OutlinedTextFieldDefaults.colors( // Customize colors for read-only state
            disabledTextColor = MaterialTheme.colorScheme.onSurface, // Keep text color normal
            disabledBorderColor = MaterialTheme.colorScheme.outline, // Standard border
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            // Ensure the container (background) color matches the input field if it's not transparent
            // disabledContainerColor = MaterialTheme.colorScheme.surface // Or Color.Transparent if input is transparent
        )
    )
}


@Preview(showBackground = true, widthDp = 400, heightDp = 600)
@Composable
fun DefaultPreviewMinimalCenteredScreenWithLogo() {
    CurrencyConverterTheme {
        MinimalCenteredConverterScreenWithLogo()
    }
}

@Preview(showBackground = true, device = "spec:width=360dp,height=720dp,dpi=480")
@Composable
fun DefaultPreviewMinimalCenteredScreenWithLogoPhone() {
    CurrencyConverterTheme {
        MinimalCenteredConverterScreenWithLogo()
    }
}