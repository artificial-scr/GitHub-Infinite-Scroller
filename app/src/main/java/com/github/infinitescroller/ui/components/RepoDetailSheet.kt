package com.github.infinitescroller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ForkRight
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.infinitescroller.data.model.Repo
import com.github.infinitescroller.ui.util.Format

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RepoDetailSheet(
    repo: Repo,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OwnerAvatar(url = repo.owner.avatarUrl, size = 56.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        repo.owner.login,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        repo.name,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (!repo.description.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(repo.description, style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(Icons.Outlined.StarOutline, Format.compactCount(repo.stars), "Stars", Modifier.weight(1f))
                StatTile(Icons.Outlined.ForkRight, Format.compactCount(repo.forks), "Forks", Modifier.weight(1f))
                StatTile(Icons.Outlined.BugReport, Format.compactCount(repo.openIssues), "Open issues", Modifier.weight(1f))
            }

            if (repo.topics.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repo.topics.forEach { TopicPill(it) }
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))

            repo.language?.let { language ->
                DetailRow(Icons.Outlined.Code, "Language") { LanguageLabel(language) }
            }
            repo.license?.label?.let { DetailRow(Icons.Outlined.Gavel, "License") { DetailValue(it) } }
            Format.date(repo.createdAt)?.let { DetailRow(Icons.Outlined.CalendarToday, "Created") { DetailValue(it) } }
            Format.relativeTime(repo.pushedAt)?.let { DetailRow(Icons.Outlined.Update, "Last push") { DetailValue(it) } }
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { context.openUrl(repo.htmlUrl) },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open on GitHub")
                }
                FilledTonalIconButton(onClick = onToggleBookmark, modifier = Modifier.size(52.dp)) {
                    Icon(
                        if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Remove from saved" else "Save repository",
                    )
                }
                FilledTonalIconButton(onClick = { context.shareRepo(repo) }, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share")
                }
            }
            repo.homepage.asWebUrl()?.let { url ->
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { context.openUrl(url) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Outlined.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Visit website")
                }
            }
        }
    }
}

@Composable
private fun StatTile(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        value()
    }
}

@Composable
private fun DetailValue(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}
