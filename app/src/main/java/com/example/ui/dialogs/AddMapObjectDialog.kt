package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LatLng
import java.util.Locale

data class ObjectTypeOption(val key: String, val label: String, val icon: ImageVector)

val OBJECT_TYPES = listOf(
  ObjectTypeOption("marker", "Point of Interest", Icons.Default.LocationOn),
  ObjectTypeOption("hazard", "Road Hazard", Icons.Default.Warning),
  ObjectTypeOption("circle", "Zone / Radius", Icons.Default.RadioButtonUnchecked),
  ObjectTypeOption("camera", "Camera", Icons.Default.CameraAlt)
)

val COLOR_OPTIONS = listOf(
  "#0284C7", // Azure
  "#EF4444", // Red
  "#F59E0B", // Amber
  "#10B981", // Emerald
  "#8B5CF6", // Violet
  "#EC4899"  // Pink
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMapObjectDialog(
  location: LatLng,
  onSave: (title: String, description: String, type: String, lat: Double, lng: Double, radius: Double, color: String) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var selectedType by remember { mutableStateOf("marker") }
  var selectedColor by remember { mutableStateOf("#0284C7") }
  var radiusMeters by remember { mutableDoubleStateOf(200.0) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("add_map_object_modal")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
        .navigationBarsPadding()
        .verticalScroll(rememberScrollState())
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.AddLocation,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Add Object to Map",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Coordinates Badge
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Position: ${String.format(Locale.US, "%.5f, %.5f", location.latitude, location.longitude)}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Object Type Selector
      Text("Object Type", fontSize = 13.sp, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        OBJECT_TYPES.forEach { opt ->
          FilterChip(
            selected = selectedType == opt.key,
            onClick = { selectedType = opt.key },
            leadingIcon = {
              Icon(opt.icon, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            label = { Text(opt.label, fontSize = 12.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
              selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Title input
      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Object Name / Label") },
        placeholder = { Text("e.g. Danger Ahead, Picnic Spot, Parking Zone") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("object_title_input")
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Description input
      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("Notes / Description (Optional)") },
        placeholder = { Text("Additional information about this object") },
        maxLines = 3,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("object_desc_input")
      )

      // Radius slider if Circle/Zone selected
      if (selectedType == "circle") {
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "Radius: ${radiusMeters.toInt()} meters",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
        Slider(
          value = radiusMeters.toFloat(),
          onValueChange = { radiusMeters = it.toDouble() },
          valueRange = 50f..2000f,
          steps = 38,
          colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary
          )
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Color picker
      Text("Pin / Highlight Color", fontSize = 13.sp, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        COLOR_OPTIONS.forEach { hex ->
          val color = Color(android.graphics.Color.parseColor(hex))
          val isSelected = selectedColor == hex
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(color)
              .clickable { selectedColor = hex }
              .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape
              ),
            contentAlignment = Alignment.Center
          ) {
            if (isSelected) {
              Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Action Button
      Button(
        onClick = {
          onSave(
            title.ifBlank { "Map Object" },
            description,
            selectedType,
            location.latitude,
            location.longitude,
            radiusMeters,
            selectedColor
          )
        },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("confirm_add_object_button")
      ) {
        Icon(Icons.Default.AddLocation, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Save Object to Map", fontWeight = FontWeight.Bold, fontSize = 16.sp)
      }

      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}
