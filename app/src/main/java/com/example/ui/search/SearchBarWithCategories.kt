package com.example.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlaceSearchResult

data class CategoryItem(val label: String, val icon: ImageVector, val query: String)

val CATEGORIES = listOf(
  CategoryItem("Gas", Icons.Default.LocalGasStation, "gas station"),
  CategoryItem("Food & Cafe", Icons.Default.LocalCafe, "cafe restaurant"),
  CategoryItem("Parking", Icons.Default.LocalParking, "parking"),
  CategoryItem("Parks", Icons.Default.Park, "park"),
  CategoryItem("Hospitals", Icons.Default.LocalHospital, "hospital")
)

@Composable
fun SearchBarWithCategories(
  query: String,
  searchResults: List<PlaceSearchResult>,
  isSearching: Boolean,
  onQueryChange: (String) -> Unit,
  onResultSelected: (PlaceSearchResult) -> Unit,
  onCategorySelected: (String) -> Unit,
  onOpenSavedPlaces: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    // Floating Search Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("search_bar_card"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = "Search",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(start = 4.dp)
        )

        OutlinedTextField(
          value = query,
          onValueChange = onQueryChange,
          placeholder = { Text("Search location, address, or landmark...", fontSize = 14.sp) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
          ),
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .testTag("search_text_input")
        )

        if (isSearching) {
          CircularProgressIndicator(
            modifier = Modifier
              .size(20.dp)
              .padding(end = 6.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
          )
        } else if (query.isNotEmpty()) {
          IconButton(onClick = { onQueryChange("") }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color.Gray)
          }
        }

        // Bookmark / Saved Places shortcut
        IconButton(
          onClick = onOpenSavedPlaces,
          modifier = Modifier.testTag("saved_places_button")
        ) {
          Icon(
            imageVector = Icons.Default.Bookmark,
            contentDescription = "Saved places",
            tint = MaterialTheme.colorScheme.primary
          )
        }
      }
    }

    // Autocomplete Dropdown List
    AnimatedVisibility(
      visible = searchResults.isNotEmpty(),
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp)
          .heightIn(max = 280.dp)
          .testTag("search_results_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
      ) {
        LazyColumn {
          items(searchResults) { result ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onResultSelected(result) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = result.name,
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = result.displayName,
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }

              Spacer(modifier = Modifier.width(8.dp))

              Icon(
                imageVector = Icons.Default.Directions,
                contentDescription = "Navigate to",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          }
        }
      }
    }

    // Category Filter Chips
    if (searchResults.isEmpty()) {
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        CATEGORIES.forEach { category ->
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 3.dp,
            modifier = Modifier
              .clickable { onCategorySelected(category.query) }
              .testTag("category_chip_${category.label}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = category.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    }
  }
}
