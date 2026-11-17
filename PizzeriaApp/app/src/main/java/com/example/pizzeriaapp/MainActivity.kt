package com.example.pizzeriaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pizzeriaapp.ui.theme.PizzeriaAppTheme
import java.text.NumberFormat
import java.util.Locale
import model.Pedido
import service.DatosPruebaFactory
import service.IngredientesPedidoService
import service.InventarioService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PizzeriaAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PantallaPizzeria()
                }
            }
        }
    }
}

@Composable
fun PantallaPizzeria() {

    // Reutilizamos los datos de prueba desarrollados en S3.
    val datos = remember {
        DatosPruebaFactory.crearDatos()
    }

    val pedido = remember(datos) {
        datos.pedidosDelDia.first()
    }

    // Inventario de demostración, mantenido en memoria.
    val inventario = remember {
        linkedMapOf(
            "Masa" to 10,
            "Salsa de tomate" to 8,
            "Queso" to 10,
            "Pepperoni" to 4
        )
    }

    val servicioInventario = remember(inventario) {
        InventarioService(inventario)
    }

    // Copia observable: Compose actualizará la pantalla al cambiar.
    var inventarioVisible by remember {
        mutableStateOf(inventario.toMap())
    }

    var estadoVisible by remember {
        mutableStateOf(pedido.estado)
    }

    var pedidoProcesado by remember {
        mutableStateOf(false)
    }

    var resultado by remember {
        mutableStateOf("Pedido pendiente de procesamiento.")
    }

    var resultadoStockInsuficiente by remember {
        mutableStateOf("Prueba de stock insuficiente no ejecutada.")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Bienvenidos a la mejor pizzería de la ciudad",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "App Móviles - Semana 4",
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        ResumenPedido(
            pedido = pedido,
            estado = estadoVisible
        )

        HorizontalDivider()

        Text(
            text = "Inventario disponible",
            style = MaterialTheme.typography.titleMedium
        )

        for ((ingrediente, cantidad) in inventarioVisible) {
            Text("$ingrediente: $cantidad")
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !pedidoProcesado,
            onClick = {

                val ingredientesNecesarios =
                    IngredientesPedidoService
                        .calcularIngredientesNecesarios(pedido.items)

                val aceptado =
                    servicioInventario
                        .descontarIngredientes(ingredientesNecesarios)

                if (aceptado) {

                    pedido.cambiarEstado("En preparación")

                    estadoVisible = pedido.estado
                    inventarioVisible = inventario.toMap()
                    pedidoProcesado = true

                    resultado =
                        "Pedido aceptado. Inventario actualizado correctamente."

                } else {

                    resultado =
                        "Pedido rechazado: ingredientes insuficientes."
                }
            }
        ) {
            Text(
                if (pedidoProcesado) {
                    "Pedido procesado"
                } else {
                    "Procesar pedido"
                }
            )
        }

        Text(
            text = resultado,
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        Text(
            text = "Prueba de inventario insuficiente",
            style = MaterialTheme.typography.titleMedium
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {

                // Escenario independiente del inventario principal.
                val inventarioPrueba = linkedMapOf(
                    "Masa" to 10,
                    "Salsa de tomate" to 8,
                    "Queso" to 10,
                    "Pepperoni" to 1
                )

                val inventarioAntes = inventarioPrueba.toMap()

                val servicioPrueba =
                    InventarioService(inventarioPrueba)

                val ingredientesNecesarios =
                    IngredientesPedidoService
                        .calcularIngredientesNecesarios(pedido.items)

                val aceptado =
                    servicioPrueba
                        .descontarIngredientes(ingredientesNecesarios)

                val sinCambios =
                    inventarioPrueba == inventarioAntes

                resultadoStockInsuficiente =
                    if (!aceptado && sinCambios) {
                        "Prueba correcta: pedido rechazado por " +
                            "stock insuficiente. Inventario sin cambios."
                    } else {
                        "Prueba fallida: revisar la integridad " +
                            "del inventario."
                    }
            }
        ) {
            Text("Probar stock insuficiente")
        }

        Text(
            text = resultadoStockInsuficiente,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun ResumenPedido(
    pedido: Pedido,
    estado: String
) {

    Text(
        text = "Pedido de ${pedido.cliente.nombre}",
        style = MaterialTheme.typography.titleLarge
    )

    Text("Correo: ${pedido.cliente.correo}")
    Text("Estado: $estado")

    for (item in pedido.items) {
        Text(
            text = "${item.producto.nombre} " +
                "x${item.cantidad} - " +
                formatearMonto(item.calcularSubtotal())
        )
    }

    Text(
        "Cantidad de pizzas: ${pedido.calcularCantidadPizzas()}"
    )

    Text(
        "Subtotal: ${formatearMonto(pedido.calcularSubtotal())}"
    )

    Text(
        "Impuesto: ${formatearMonto(pedido.calcularImpuesto())}"
    )

    Text(
        "Descuento: ${formatearMonto(pedido.calcularDescuento())}"
    )

    Text(
        text = "Total: ${formatearMonto(pedido.calcularTotal())}",
        style = MaterialTheme.typography.titleMedium
    )
}

private fun formatearMonto(monto: Double): String {
    val formato = NumberFormat.getCurrencyInstance(
        Locale.forLanguageTag("es-CL")
    )

    return formato.format(monto)
}