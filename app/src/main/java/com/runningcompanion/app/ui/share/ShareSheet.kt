package com.runningcompanion.app.ui.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.runningcompanion.app.domain.model.Session
import com.runningcompanion.app.domain.share.CardTemplate
import com.runningcompanion.app.domain.share.ShareCardModel
import com.runningcompanion.app.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ShareSheet(
    session: Session,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val model = remember(session) { ShareCardModel.fromSession(session) }
    var template by remember { mutableStateOf(CardTemplate.FULL) }
    val imageBitmap = remember(model, template) {
        ShareCardRenderer.render(model, template).asImageBitmap()
    }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun runAction(message: String, action: suspend () -> String) {
        scope.launch {
            busy = true
            status = message
            try {
                status = action()
            } catch (e: Exception) {
                status = "Failed: ${e.message}"
            }
            busy = false
        }
    }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Card(
            modifier = Modifier.width(340.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Share workout",
                    fontSize = 16.sp,
                    color = AppColors.Ink
                )
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CardTemplate.values().forEach { t ->
                        FilterChip(
                            selected = template == t,
                            onClick = { if (!busy) template = t },
                            label = { Text(t.label, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Share card preview for ${session.workoutSnapshot.name}",
                    modifier = Modifier.width(300.dp)
                )
                Spacer(Modifier.height(10.dp))

                status?.let {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = AppColors.InkMuted
                    )
                    Spacer(Modifier.height(6.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            runAction("Exporting GPX…") {
                                val file = withContext(Dispatchers.IO) {
                                    ShareActions.writeGpx(context, session)
                                }
                                ShareActions.shareGpx(context, file, session.workoutSnapshot.name)
                                "GPX shared"
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("GPX", fontSize = 14.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            runAction("Saving…") {
                                val file = withContext(Dispatchers.IO) {
                                    ShareActions.writeCardPng(context, session, template)
                                }
                                "Saved to " + withContext(Dispatchers.IO) {
                                    ShareActions.savePng(context, file, session.workoutSnapshot.name)
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", fontSize = 14.sp)
                    }
                    Button(
                        onClick = {
                            runAction("Sharing…") {
                                val file = withContext(Dispatchers.IO) {
                                    ShareActions.writeCardPng(context, session, template)
                                }
                                ShareActions.shareImage(context, file, session.workoutSnapshot.name)
                                "Image shared"
                            }
                        },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Run),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Share", fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, enabled = !busy) {
                        Text("Done", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
