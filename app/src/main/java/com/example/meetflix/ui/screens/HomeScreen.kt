package com.example.meetflix.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import android.net.Uri
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meetflix.R
import com.example.meetflix.ui.components.MFXButton
import com.example.meetflix.ui.theme.DarkGrayColor
import com.example.meetflix.ui.theme.Geist_Font
import com.example.meetflix.ui.theme.PrimaryColor
import com.example.meetflix.ui.theme.PurpleBtnBGColor
import com.example.meetflix.ui.theme.PurpleBtnBorderColor
import com.example.meetflix.ui.theme.Zinc700Color
import com.arthenica.ffmpegkit.FFmpegKit
import com.example.meetflix.ui.theme.BloodRed
import com.example.meetflix.ui.theme.PrimaryBtnAlpha

@Composable
fun HomeScreen(innerPadding: PaddingValues) {

    val context = LocalContext.current
    var pickedFileName by remember { mutableStateOf<String?>(null) }
    var pickedFileUri by remember { mutableStateOf<Uri?>(null) }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pickedFileUri = it
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
            // this for safe area
            .padding(innerPadding)
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Upload & Stream",
            color = PrimaryColor,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            fontFamily = Geist_Font,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Drag and drop your files or click to browse. Upload your documents, images, and files securely.\n",
            color = Color.Gray,
            fontSize = 16.sp,
            fontFamily = Geist_Font,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .border(0.1.dp, Zinc700Color, RoundedCornerShape(16.dp))
                .background(DarkGrayColor)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (pickedFileName != null) {
                Column (
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
                        "$pickedFileName\"",
                        color = Color.White,
                        textAlign = TextAlign.Center

                    )
                    MFXButton(
                        text = "Make it Sharable",
                        modifier = Modifier.fillMaxWidth(),
                        bgColor = PrimaryColor,
                        bgAlpha = PrimaryBtnAlpha,
                        borderColor = PrimaryColor,
                        borderSize = 0.5.dp,
                        textColor = Color.White,
                        icon = R.drawable.share,
                        onClick = {
                        }
                    )
                }
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.cloudupload),
                    contentDescription = "Upload Icon",
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Upload your files",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Drag and drop your files here, or click to browse",
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(20.dp))
                MFXButton(
                    text = "Browse Files",
                    modifier = Modifier.fillMaxWidth(),
                    bgColor = PrimaryColor,
                    bgAlpha = PrimaryBtnAlpha,
                    borderColor = PrimaryColor,
                    borderSize = 0.5.dp,
                    textColor = Color.White,
                    onClick = {
                        pickFileLauncher.launch("video/mp4")
                    }
                )
            }
        }
    }
}

fun android.database.Cursor.getColumnIndexOpenableColumnsDisplayName(): Int {
    return getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
}
