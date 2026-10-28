package com.example.ui.trip

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TripState
import com.example.data.model.TripSummary
import java.util.Locale

@Composable
fun TripControlBar(
  tripState: TripState,
  onStartTrip: () -> Unit,
  onPauseResumeTrip: () -> Unit,
  onStopTrip: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(modifier = modifier) {
    if (!tripState.isActive) {
      // Floating Start Trip Button
      ExtendedFloatingActionButton(
        onClick = onStartTrip,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color(0xFF10B981),
        contentColor = Color.White,
        elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(6.dp),
        modifier = Modifier
          .testTag("start_trip_fab")
      ) {
        Icon(
          imageVector = Icons.Default.PlayArrow,
          contentDescription = "Start Trip",
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Start Trip",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp
        )
      }
    } else {
      // Ongoing Trip HUD Dashboard
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .testTag("active_trip_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          // Status Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = CircleShape,
                color = if (tripState.isPaused) Color(0xFFF59E0B) else Color(0xFF10B981),
                modifier = Modifier.size(10.dp)
              ) {}
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (tripState.isPaused) "TRIP PAUSED" else "ON-GOING TRIP",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (tripState.isPaused) Color(0xFFF59E0B) else Color(0xFF10B981),
                letterSpacing = 1.sp
              )
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF1E293B)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  Icons.Default.Timer,
                  contentDescription = null,
                  tint = Color(0xFF94A3B8),
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = formatTripTime(tripState.elapsedSeconds),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Live Metrics: Prominent Distance in KM
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
          ) {
            Column {
              Text(
                text = "DISTANCE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
              )
              Row(verticalAlignment = Alignment.Bottom) {
                Text(
                  text = String.format(Locale.US, "%.2f", tripState.distanceMeters / 1000.0),
                  fontSize = 36.sp,
                  fontWeight = FontWeight.Black,
                  color = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "km",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFE2E8F0)
                )
              }
            }

            // Speed metrics
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "SPEED",
                  fontSize = 10.sp,
                  color = Color(0xFF94A3B8),
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "${tripState.currentSpeedKmh.toInt()} km/h",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "AVG",
                  fontSize = 10.sp,
                  color = Color(0xFF94A3B8),
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = String.format(Locale.US, "%.1f km/h", tripState.averageSpeedKmh),
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFE2E8F0)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Action Buttons: Pause/Resume, Stop Trip
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = onPauseResumeTrip,
              colors = ButtonDefaults.buttonColors(
                containerColor = if (tripState.isPaused) Color(0xFF10B981) else Color(0xFF1E293B)
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("pause_resume_trip_button")
            ) {
              Icon(
                imageVector = if (tripState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = null,
                tint = Color.White
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (tripState.isPaused) "Resume" else "Pause",
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }

            Button(
              onClick = onStopTrip,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("stop_trip_button")
            ) {
              Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
              Spacer(modifier = Modifier.width(6.dp))
              Text("End Trip", fontWeight = FontWeight.Bold, color = Color.White)
            }
          }
        }
      }
    }
  }
}

@Composable
fun TripSummaryDialog(
  summary: TripSummary,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          shape = CircleShape,
          color = Color(0xFF10B981),
          modifier = Modifier.size(36.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
          }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text("Trip Completed!", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text("Here is your trip summary:", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(14.dp))

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Total Distance:", fontWeight = FontWeight.Medium)
              Text(
                text = String.format(Locale.US, "%.2f km", summary.totalDistanceKm),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Duration:", fontWeight = FontWeight.Medium)
              Text(summary.durationFormatted, fontWeight = FontWeight.Bold)
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Average Speed:", fontWeight = FontWeight.Medium)
              Text(
                text = String.format(Locale.US, "%.1f km/h", summary.avgSpeedKmh),
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.testTag("dismiss_trip_summary_button")
      ) {
        Text("Done")
      }
    }
  )
}

fun formatTripTime(seconds: Long): String {
  val hrs = seconds / 3600
  val mins = (seconds % 3600) / 60
  val secs = seconds % 60
  return if (hrs > 0) {
    String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
  } else {
    String.format(Locale.US, "%02d:%02d", mins, secs)
  }
}
