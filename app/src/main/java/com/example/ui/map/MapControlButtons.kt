package com.example.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun MapControlButtons(
  onZoomIn: () -> Unit,
  onZoomOut: () -> Unit,
  onRecenter: () -> Unit,
  onAddObject: () -> Unit,
  onOpenObjectsList: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    horizontalAlignment = Alignment.End
  ) {
    // Add Map Object FAB (Highlighted in Primary Cyan)
    FloatingActionButton(
      onClick = onAddObject,
      shape = CircleShape,
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      elevation = FloatingActionButtonDefaults.elevation(5.dp),
      modifier = Modifier
        .size(48.dp)
        .testTag("fab_add_object")
    ) {
      Icon(Icons.Default.AddLocation, contentDescription = "Add Object to Map")
    }

    // View All Map Objects FAB
    FloatingActionButton(
      onClick = onOpenObjectsList,
      shape = CircleShape,
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.onSurface,
      elevation = FloatingActionButtonDefaults.elevation(4.dp),
      modifier = Modifier
        .size(48.dp)
        .testTag("fab_view_objects")
    ) {
      Icon(Icons.AutoMirrored.Filled.List, contentDescription = "View Map Objects")
    }

    // Zoom +/- combined card
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 4.dp
    ) {
      Column {
        FloatingActionButton(
          onClick = onZoomIn,
          shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.onSurface,
          elevation = FloatingActionButtonDefaults.elevation(0.dp),
          modifier = Modifier
            .size(48.dp)
            .testTag("fab_zoom_in")
        ) {
          Icon(Icons.Default.Add, contentDescription = "Zoom In")
        }

        FloatingActionButton(
          onClick = onZoomOut,
          shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.onSurface,
          elevation = FloatingActionButtonDefaults.elevation(0.dp),
          modifier = Modifier
            .size(48.dp)
            .testTag("fab_zoom_out")
        ) {
          Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
        }
      }
    }

    // My Location FAB
    FloatingActionButton(
      onClick = onRecenter,
      shape = CircleShape,
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      elevation = FloatingActionButtonDefaults.elevation(6.dp),
      modifier = Modifier
        .size(54.dp)
        .testTag("fab_my_location")
    ) {
      Icon(Icons.Default.MyLocation, contentDescription = "My Location")
    }
  }
}
