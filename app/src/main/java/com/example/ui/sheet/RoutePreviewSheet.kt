package com.example.ui.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RouteInfo
import com.example.data.model.TravelMode
import com.example.ui.navigation.formatDistance
import com.example.ui.navigation.formatDuration
import com.example.ui.navigation.getManeuverIcon

@Composable
fun RoutePreviewSheet(
  originName: String,
  destinationName: String,
  travelMode: TravelMode,
  routeInfo: RouteInfo?,
  isCalculating: Boolean,
  onModeSelected: (TravelMode) -> Unit,
  onStartNavigation: () -> Unit,
  onSavePlace: () -> Unit,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showItinerary by remember { mutableStateOf(false) }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(16.dp)
      .testTag("route_preview_sheet"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      // Header: Origin -> Destination and Close button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Route to",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
          )
          Text(
            text = destinationName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Row {
          IconButton(onClick = onSavePlace, modifier = Modifier.testTag("bookmark_destination_button")) {
            Icon(Icons.Default.BookmarkBorder, contentDescription = "Bookmark place", tint = MaterialTheme.colorScheme.primary)
          }
          IconButton(onClick = onCancel, modifier = Modifier.testTag("close_preview_button")) {
            Icon(Icons.Default.Close, contentDescription = "Close preview")
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Travel Mode Selector (Drive, Bike, Walk)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf(
          Triple(TravelMode.DRIVING, Icons.Default.DirectionsCar, "Drive"),
          Triple(TravelMode.CYCLING, Icons.AutoMirrored.Filled.DirectionsBike, "Bike"),
          Triple(TravelMode.WALKING, Icons.AutoMirrored.Filled.DirectionsWalk, "Walk")
        ).forEach { (mode, icon, label) ->
          FilterChip(
            selected = travelMode == mode,
            onClick = { onModeSelected(mode) },
            leadingIcon = {
              Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            label = { Text(label, fontWeight = FontWeight.SemiBold) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
              selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Route Calculation State
      if (isCalculating) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
          Spacer(modifier = Modifier.width(10.dp))
          Text("Calculating best route...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      } else if (routeInfo != null) {
        // Route Summary Bar
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = formatDuration(routeInfo.durationSeconds),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "${formatDistance(routeInfo.distanceMeters)} - fastest route via OpenStreetMap",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            TextButton(onClick = { showItinerary = !showItinerary }) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (showItinerary) "Hide Steps" else "Steps")
            }
          }
        }

        // Expandable Itinerary Steps
        AnimatedVisibility(visible = showItinerary) {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(180.dp)
              .padding(top = 10.dp)
          ) {
            itemsIndexed(routeInfo.steps) { index, step ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  modifier = Modifier.size(28.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = getManeuverIcon(step.maneuverType, step.maneuverModifier),
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(step.instruction, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                  if (step.distanceMeters > 0) {
                    Text(
                      formatDistance(step.distanceMeters),
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = onStartNavigation,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("start_navigation_button")
        ) {
          Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Start Navigation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      }
    }
  }
}
