package com.meetflix.upload.presentation.screen

import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meetflix.R
import com.meetflix.core.ui.components.MFXButton
import com.meetflix.core.ui.theme.DarkGrayColor
import com.meetflix.core.ui.theme.Geist_Font
import com.meetflix.core.ui.theme.PrimaryColor
import com.meetflix.core.ui.theme.blackRedGradient

@Composable
fun UploadScreen(innerPadding: PaddingValues) {
    val context = LocalContext.current
    val boxShape = RoundedCornerShape(16.dp)
    var pickedFileName by remember { mutableStateOf<String?>(null) }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                val nameIndex = c.getColumnIndexOpenableColumnsDisplayName()
                if (nameIndex != -1 && c.moveToFirst()) {
                    pickedFileName = c.getString(nameIndex)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(50.dp))
        Text(
            text = stringResource(R.string.upload_and_stream),
            color = PrimaryColor,
            fontWeight = FontWeight.Bold,
            fontSize = 25.sp,
            fontFamily = Geist_Font,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.upload_and_stream_desc),
            color = Color.Gray,
            fontSize = 12.sp,
            fontFamily = Geist_Font,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .clip(boxShape)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = boxShape
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            DarkGrayColor.copy(alpha = 0.78f),
                            Color.Black.copy(alpha = 0.42f)
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (pickedFileName != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    Text(
                        "\uD83C\uDFAC",
                        color = Color.White,
                        fontSize = 30.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        pickedFileName.orEmpty(),
                        color = Color.White,
                        textAlign = TextAlign.Center

                    )
                    MFXButton(
                        text = stringResource(R.string.make_it_sharable),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(blackRedGradient)
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.35f),
                                        Color.Red.copy(alpha = 0.08f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        textColor = Color.White,
                        icon = R.drawable.share,
                        onClick = {
                        }
                    )
                }
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.cloudupload),
                    contentDescription = stringResource(R.string.cd_upload_icon),
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.upload_your_files),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontFamily = Geist_Font,
                    fontSize = 18.sp,
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = stringResource(R.string.upload_files_help),
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontFamily = Geist_Font,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                MFXButton(
                    text = stringResource(R.string.browse_files),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(blackRedGradient)
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.Red.copy(alpha = 0.08f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    textColor = Color.White,
                    onClick = {
                        pickFileLauncher.launch("video/mp4")
                    }
                )
            }
        }
    }
}

fun Cursor.getColumnIndexOpenableColumnsDisplayName(): Int {
    return getColumnIndex(OpenableColumns.DISPLAY_NAME)
}
