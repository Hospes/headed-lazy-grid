package ua.hospes.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ua.hospes.lazygrid.*
import ua.hospes.sample.ui.theme.LazyGridTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LazyGridTheme {
                Grid(
                    items = testItems,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun Grid(
    modifier: Modifier = Modifier,
    items: List<String>,
    lazyGridState: LazyGridState = rememberLazyGridState(),
) {
    LazyVerticalGrid(
        state = lazyGridState,
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier,
    ) {
        items(items = items.take(1)) {
            Item(
                item = it,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        stickyHeader(key = "header") {
            Text(
                text = "TEST HEADER", modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color.Red)
            )
        }

        items(items = items) {
            Item(
                item = it,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        stickyHeader(key = "header2") {
            Text(
                text = "TEST HEADER", modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color.Green)
            )
        }

        items(items = items) {
            Item(
                item = it,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Item(
    item: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .border(width = 1.dp, color = Color.Gray),
    ) {
        Text(text = item, modifier = Modifier.align(Alignment.Center))
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    LazyGridTheme {
        Grid(
            items = testItems,
            modifier = Modifier.fillMaxSize(),
        )
    }
}


private val testItems = listOf<String>(
    "Item", "5yP1", "A54", "C0RMjL", "1kW", "5fI4ZCaJ", "0PgNQ7t",
    "j2Os", "1CL", "3BruWhs", "Kq0I8FP", "1Qf", "VBt", "k4g18L", "9yQ4", "O906cPso", "w4F3Ec6",
    "tBf", "4mCILC", "1733rG7", "25Lw", "0xnA", "9G02", "cho", "M3aC5", "d1UOJ", "6N2G8B",
)