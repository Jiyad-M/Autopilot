package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.TurnSlightLeft
import androidx.compose.material.icons.filled.TurnSlightRight
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.data.model.NavigationProgress
import com.example.data.model.NavigationStatus
import com.example.data.model.RouteStep
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NavigationHud(
  navStatus: NavigationStatus,
  progress: NavigationProgress?,
  isSimulating: Boolean,
  simulationSpeed: Float,
  isMuted: Boolean,
  isFollowingUser: Boolean,
  onSpeedSelected: (Float) -> Unit,
  onToggleMute: () -> Unit,
  onToggleFollow: () -> Unit,
  onRecenter: () -> Unit,
  onStopNavigation: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(modifier = modifier.fillMaxSize()) {
    // Top Turn Maneuver Banner
    AnimatedVisibility(
      visible = navStatus == NavigationStatus.NAVIGATING && progress != null,
      enter = slideInVertically { -it } + fadeIn(),
      exit = slideOutVertically { -it } + fadeOut(),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .statusBarsPadding()
        .padding(16.dp)
    ) {
      if (progress != null) {
        TopInstructionCard(
          step = progress.currentStep,
          distanceMeters = progress.distanceToNextManeuverMeters,
          nextStep = progress.nextStep
        )
      }
    }

    // Bottom Navigation Bar
    AnimatedVisibility(
      visible = navStatus == NavigationStatus.NAVIGATING && progress != null,
      enter = slideInVertically { it } + fadeIn(),
      exit = slideOutVertically { it } + fadeOut(),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      if (progress != null) {
        BottomNavigationDashboard(
          progress = progress,
          isSimulating = isSimulating,
          simulationSpeed = simulationSpeed,
          isMuted = isMuted,
          isFollowingUser = isFollowingUser,
          onSpeedSelected = onSpeedSelected,
          onToggleMute = onToggleMute,
          onToggleFollow = onToggleFollow,
          onRecenter = onRecenter,
          onStopNavigation = onStopNavigation
        )
      }
    }

    // Arrival Celebration Dialog
    if (navStatus == NavigationStatus.ARRIVED) {
      AlertDialog(
        onDismissRequest = onStopNavigation,
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Flag,
              contentDescription = null,
              tint = Color(0xFF10B981),
              modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("You Have Arrived!", fontWeight = FontWeight.Bold)
          }
        },
        text = {
          Text("You have reached your destination. Thank you for navigating with Leaflet Nav.")
        },
        confirmButton = {
          Button(
            onClick = onStopNavigation,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            modifier = Modifier.testTag("finish_nav_button")
          ) {
            Text("Done")
          }
        }
      )
    }
  }
}

@Composable
fun TopInstructionCard(
  step: RouteStep,
  distanceMeters: Double,
  nextStep: RouteStep?,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("turn_instruction_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Maneuver Icon Box
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF0284C7),
          modifier = Modifier.size(56.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = getManeuverIcon(step.maneuverType, step.maneuverModifier),
              contentDescription = step.instruction,
              tint = Color.White,
              modifier = Modifier.size(34.dp)
            )
          }
        }

        // Distance and main instruction
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = formatDistance(distanceMeters),
            color = Color(0xFF38BDF8),
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold
          )
          Text(
            text = step.instruction,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      // Next step preview
      if (nextStep != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Text(
            text = "Then: ",
            color = Color(0xFF94A3B8),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
          )
          Icon(
            imageVector = getManeuverIcon(nextStep.maneuverType, nextStep.maneuverModifier),
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = nextStep.instruction,
            color = Color(0xFFE2E8F0),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

@Composable
fun BottomNavigationDashboard(
  progress: NavigationProgress,
  isSimulating: Boolean,
  simulationSpeed: Float,
  isMuted: Boolean,
  isFollowingUser: Boolean,
  onSpeedSelected: (Float) -> Unit,
  onToggleMute: () -> Unit,
  onToggleFollow: () -> Unit,
  onRecenter: () -> Unit,
  onStopNavigation: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("nav_bottom_dashboard"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // Top row: ETA, Remaining Time, Remaining Distance, Speed
      val etaTime = remember(progress.remainingDurationSeconds) {
        val arrivalMillis = System.currentTimeMillis() + (progress.remainingDurationSeconds * 1000).toLong()
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(arrivalMillis))
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = formatDuration(progress.remainingDurationSeconds),
              color = Color(0xFF38BDF8),
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "ETA $etaTime",
              color = Color(0xFF94A3B8),
              fontSize = 13.sp
            )
          }
          Text(
            text = formatDistance(progress.remainingDistanceMeters) + " remaining",
            color = Color(0xFFCBD5E1),
            fontSize = 13.sp
          )
        }

        // Speed badge
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFF1E293B)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Speed,
              contentDescription = null,
              tint = Color(0xFF38BDF8),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${progress.currentSpeedKmh.toInt()} km/h",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }
        }
      }

      // Simulation Speed Controls (if simulating)
      if (isSimulating) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "Sim Speed:",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
          listOf(1f, 2f, 4f).forEach { speed ->
            FilterChip(
              selected = simulationSpeed == speed,
              onClick = { onSpeedSelected(speed) },
              label = { Text("${speed.toInt()}x", fontSize = 12.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF0284C7),
                selectedLabelColor = Color.White,
                containerColor = Color(0xFF1E293B),
                labelColor = Color(0xFF94A3B8)
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.height(32.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Action buttons row: Stop (Red), Follow Camera, Mute, Recenter
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Red Exit / Stop Route button
        Button(
          onClick = onStopNavigation,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("stop_nav_button")
        ) {
          Icon(Icons.Default.Close, contentDescription = "End Route", tint = Color.White)
          Spacer(modifier = Modifier.width(6.dp))
          Text("End Route", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          // Camera follow
          IconButton(
            onClick = onToggleFollow,
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(if (isFollowingUser) Color(0xFF0284C7) else Color(0xFF1E293B))
          ) {
            Icon(
              imageVector = if (isFollowingUser) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
              contentDescription = "Follow location",
              tint = Color.White
            )
          }

          // Audio Mute toggle
          IconButton(
            onClick = onToggleMute,
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(Color(0xFF1E293B))
          ) {
            Icon(
              imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
              contentDescription = "Audio cues",
              tint = if (isMuted) Color(0xFF94A3B8) else Color(0xFF38BDF8)
            )
          }

          // Recenter
          IconButton(
            onClick = onRecenter,
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(Color(0xFF1E293B))
              .testTag("recenter_button")
          ) {
            Icon(
              imageVector = Icons.Default.NearMe,
              contentDescription = "Recenter",
              tint = Color.White
            )
          }
        }
      }
    }
  }
}

fun formatDistance(meters: Double): String {
  return if (meters < 1000) {
    "${meters.toInt()} m"
  } else {
    String.format(Locale.US, "%.1f km", meters / 1000.0)
  }
}

fun formatDuration(seconds: Double): String {
  val totalMins = (seconds / 60).toInt()
  return when {
    totalMins < 1 -> "< 1 min"
    totalMins < 60 -> "$totalMins min"
    else -> {
      val hrs = totalMins / 60
      val mins = totalMins % 60
      "${hrs}h ${mins}m"
    }
  }
}

fun getManeuverIcon(type: String, modifier: String?): ImageVector {
  if (type == "arrive") return Icons.Default.Flag
  return when (modifier) {
    "left" -> Icons.Default.TurnLeft
    "slight left" -> Icons.Default.TurnSlightLeft
    "sharp left" -> Icons.Default.TurnLeft
    "right" -> Icons.Default.TurnRight
    "slight right" -> Icons.Default.TurnSlightRight
    "sharp right" -> Icons.Default.TurnRight
    "uturn" -> Icons.AutoMirrored.Filled.RotateLeft
    "straight" -> Icons.Default.Navigation
    else -> when (type) {
      "depart" -> Icons.Default.Navigation
      "fork" -> Icons.Default.TurnSlightRight
      "merge" -> Icons.Default.Navigation
      else -> Icons.Default.Navigation
    }
  }
}
