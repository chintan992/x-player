package com.chintan992.xplayer.cast

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object WearableHelper {

    private var server: LocalVideoServer? = null

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    private const val CAPABILITY_WEAR_PLAYER = "wear_video_player"

    suspend fun getConnectedVideoPlayers(context: Context): List<com.google.android.gms.wearable.Node> {
        return try {
            val capabilityInfo = Tasks.await(
                Wearable.getCapabilityClient(context)
                    .getCapability(CAPABILITY_WEAR_PLAYER, com.google.android.gms.wearable.CapabilityClient.FILTER_REACHABLE)
            )
            capabilityInfo.nodes.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun castVideoToWatch(context: Context, videoUri: Uri, title: String, targetNodeId: String?) {
        withContext(Dispatchers.IO) {
            val scheme = videoUri.scheme
            val streamUrl = if (scheme == "http" || scheme == "https") {
                videoUri.toString()
            } else {
                server?.stop()
                server = LocalVideoServer(context, videoUri)
                try {
                    server?.start()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to start local server: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    return@withContext
                }

                val ip = getLocalIpAddress()
                if (ip == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Connect to Wi-Fi to cast local content", Toast.LENGTH_SHORT).show()
                    }
                    return@withContext
                }
                "http://$ip:8080/stream"
            }
            
            // streamUrl is String, so null check is redundant/removed

            try {
                // Create simple JSON payload
                val payload = "{\"url\":\"$streamUrl\",\"title\":\"$title\"}"
                
                val targetNode = if (targetNodeId != null) {
                    // Use specifically requested node
                    targetNodeId
                } else {
                    // Auto-select: explicit capability first, then fallback to any node (for backward compat)
                    val capableNodes = getConnectedVideoPlayers(context)
                    if (capableNodes.isNotEmpty()) {
                        capableNodes.first().id
                    } else {
                        val allNodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
                        allNodes.firstOrNull()?.id
                    }
                }
                
                if (targetNode != null) {
                    val messageClient = Wearable.getMessageClient(context)
                    Tasks.await(messageClient.sendMessage(targetNode, "/play_stream", payload.toByteArray()))
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Casting to watch...", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No watch found. Install the app on your watch.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    fun stopCasting() {
        server?.stop()
        server = null
    }
}
