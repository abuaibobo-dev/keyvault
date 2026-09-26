package com.keyvault.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.keyvault.app.data.KeyItem
import com.keyvault.app.data.NoteItem
import com.keyvault.app.data.TagParser
import com.keyvault.app.ui.components.SensitiveClipboard
import com.keyvault.app.ui.theme.glass

@Composable
fun KeyEditor(model: AppViewModel, existing: KeyItem?, onBack: () -> Unit) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var value by remember(existing?.id) { mutableStateOf(existing?.value ?: "") }
    var note by remember(existing?.id) { mutableStateOf(existing?.note ?: "") }
    var tags by remember(existing?.id) { mutableStateOf(existing?.tags?.joinToString(", ") ?: "") }
    var category by remember(existing?.id) { mutableStateOf(existing?.category ?: "") }
    var pinned by remember(existing?.id) { mutableStateOf(existing?.pinned ?: false) }
    var deletePrompt by remember { mutableStateOf(false) }
    var validation by remember { mutableStateOf("") }
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(if (existing == null) "新建密钥" else "编辑密钥", style = MaterialTheme.typography.headlineMedium)
        Column(Modifier.fillMaxWidth().glass().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            SecretField(value, { value = it }, "密钥值")
            TextButton(onClick = { SensitiveClipboard.copy(context, value); Toast.makeText(context, "已复制，30 秒后清除", Toast.LENGTH_SHORT).show() }, enabled = value.isNotEmpty()) { Text("复制密钥值") }
            OutlinedTextField(note, { note = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            OutlinedTextField(tags, { tags = it }, label = { Text("标签（逗号分隔）") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(category, { category = it }, label = { Text("分类") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("置顶", Modifier.weight(1f)); Switch(checked = pinned, onCheckedChange = { pinned = it })
            }
        }
        if (validation.isNotEmpty()) Text(validation, color = MaterialTheme.colorScheme.error)
        Button(onClick = {
            if (name.isBlank()) { validation = "请输入名称"; return@Button }
            if (value.isBlank()) { validation = "请输入密钥值"; return@Button }
            model.saveKey((existing ?: KeyItem(name = name.trim(), value = value)).copy(
                name = name.trim(), value = value, note = note, tags = TagParser.parse(tags), category = category.trim(),
                pinned = pinned, updatedAt = System.currentTimeMillis(),
            ))
            onBack()
        }, modifier = Modifier.fillMaxWidth()) { Text("保存") }
        if (existing != null) OutlinedButton(onClick = { deletePrompt = true }, modifier = Modifier.fillMaxWidth()) { Text("删除密钥") }
        TextButton(onClick = onBack) { Text("返回") }
    }
    if (deletePrompt && existing != null) AlertDialog(onDismissRequest = { deletePrompt = false }, title = { Text("删除密钥？") },
        text = { Text("此操作无法撤销。") }, confirmButton = { TextButton(onClick = { model.deleteKey(existing.id); onBack() }) { Text("删除") } },
        dismissButton = { TextButton(onClick = { deletePrompt = false }) { Text("取消") } })
}

@Composable
fun NoteEditor(model: AppViewModel, existing: NoteItem?, onBack: () -> Unit) {
    var title by remember(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var body by remember(existing?.id) { mutableStateOf(existing?.body ?: "") }
    var tags by remember(existing?.id) { mutableStateOf(existing?.tags?.joinToString(", ") ?: "") }
    var category by remember(existing?.id) { mutableStateOf(existing?.category ?: "") }
    var pinned by remember(existing?.id) { mutableStateOf(existing?.pinned ?: false) }
    var favorite by remember(existing?.id) { mutableStateOf(existing?.favorite ?: false) }
    var deletePrompt by remember { mutableStateOf(false) }
    var validation by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(if (existing == null) "新建笔记" else "编辑笔记", style = MaterialTheme.typography.headlineMedium)
        Column(Modifier.fillMaxWidth().glass().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(body, { body = it }, label = { Text("正文") }, modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp), minLines = 7)
            OutlinedTextField(tags, { tags = it }, label = { Text("标签（逗号分隔）") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(category, { category = it }, label = { Text("分类") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("置顶", Modifier.weight(1f)); Switch(pinned, { pinned = it })
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("收藏", Modifier.weight(1f)); Switch(favorite, { favorite = it })
            }
        }
        if (validation.isNotEmpty()) Text(validation, color = MaterialTheme.colorScheme.error)
        Button(onClick = {
            if (title.isBlank()) { validation = "请输入标题"; return@Button }
            model.saveNote((existing ?: NoteItem(title = title.trim(), body = body)).copy(
                title = title.trim(), body = body, tags = TagParser.parse(tags), category = category.trim(),
                pinned = pinned, favorite = favorite, updatedAt = System.currentTimeMillis(),
            ))
            onBack()
        }, modifier = Modifier.fillMaxWidth()) { Text("保存") }
        if (existing != null) OutlinedButton(onClick = { deletePrompt = true }, modifier = Modifier.fillMaxWidth()) { Text("删除笔记") }
        TextButton(onClick = onBack) { Text("返回") }
    }
    if (deletePrompt && existing != null) AlertDialog(onDismissRequest = { deletePrompt = false }, title = { Text("删除笔记？") },
        text = { Text("此操作无法撤销。") }, confirmButton = { TextButton(onClick = { model.deleteNote(existing.id); onBack() }) { Text("删除") } },
        dismissButton = { TextButton(onClick = { deletePrompt = false }) { Text("取消") } })
}
