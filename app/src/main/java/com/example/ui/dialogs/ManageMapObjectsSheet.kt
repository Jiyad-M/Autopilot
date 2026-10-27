package com.example.ui.dialogs

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.MapObjectEntity
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageMapObjectsSheet(
  objects: List<MapObjectEntity>,
  onObjectSelected: (MapObjectEntity) -> Unit,
  onNavigateTo: (MapObjectEntity) -> Unit,
  onDeleteObject: (MapObjectEntity) -> Unit,
  onAddNewObject: () -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("manage_objects_modal")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
        .navigationBarsPadding()
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Map Objects (${objects.size})",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Button(
        onClick = {
          onDismiss()
          onAddNewObject()
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("add_new_object_list_button")
      ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Add New Object at Location")
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (objects.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              "No custom objects on map yet",
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              "Tap '+ Object' to drop markers, hazards, or zones",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.outline
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          items(objects) { obj ->
            val color = try {
              Color(android.graphics.Color.parseColor(obj.colorHex))
            } catch (e: Exception) {
              MaterialTheme.colorScheme.primary
            }

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onObjectSelected(obj) }
                .padding(vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier.size(38.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  val iconText = when (obj.objectType) {
                    "hazard" -> "⚠️"
                    "circle", "zone" -> "⭕"
                    "camera" -> "📷"
                    else -> "📍"
                  }
                  Text(iconText, fontSize = 16.sp)
                }
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = obj.title,
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = if (obj.description.isNotBlank()) obj.description else String.format(Locale.US, "%.4f, %.4f", obj.latitude, obj.longitude),
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }

              IconButton(
                onClick = { onNavigateTo(obj) },
                modifier = Modifier.testTag("navigate_obj_${obj.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.Directions,
                  contentDescription = "Navigate",
                  tint = MaterialTheme.colorScheme.primary
                )
              }

              IconButton(onClick = { onDeleteObject(obj) }) {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = "Delete",
                  tint = MaterialTheme.colorScheme.outline
                )
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
          }
        }
      }
    }
  }
}
