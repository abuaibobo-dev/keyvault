package com.keyvault.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.keyvault.app.data.KeyItem
import com.keyvault.app.data.NoteItem
import com.keyvault.app.ui.theme.LocalAccent
import com.keyvault.app.ui.theme.cyanAccent
import com.keyvault.app.ui.theme.glass
import com.keyvault.app.util.SearchFilter

@Composable
fun AppRoot(
    model: AppViewModel,
    biometricAvailable: Boolean,
    biometricHardwareAvailable: Boolean,
    onBiometricUnlock: () -> Unit,
    onEnableBiometric: (CharArray, (Boolean) -> Unit) -> Unit,
    onDisableBiometric: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onTheme: (String) -> Unit,
    onAccent: (Int) -> Unit,
    themeMode: String,
    accentIndex: Int,
) {
    val vault by model.vault.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    if (vault == null) {
        UnlockScreen(model.firstRun, busy, error, biometricAvailable && model.settings.biometricEnabled, onBiometricUnlock, model::unlock)
        return
    }
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route ?: "keys"
    val rootRoutes = listOf("keys", "notes", "generator", "settings")
    val root = route in rootRoutes
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            if (root) NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)) {
                listOf(
                    Triple("keys", "密钥", Icons.Filled.Key),
                    Triple("notes", "笔记", Icons.Filled.Description),
                    Triple("generator", "生成器", Icons.Filled.AutoAwesome),
                    Triple("settings", "设置", Icons.Filled.Settings),
                ).forEach { (destination, label, icon) ->
                    NavigationBarItem(
                        selected = route == destination,
                        onClick = { nav.navigate(destination) { popUpTo("keys") { saveState = true }; launchSingleTop = true; restoreState = true } },
                        icon = { Icon(icon, contentDescription = label) }, label = { Text(label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (route == "keys" || route == "notes") FloatingActionButton(onClick = {
                nav.navigate(if (route == "keys") "key/new" else "note/new")
            }) { Icon(Icons.Filled.Add, contentDescription = "新建") }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (error.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(error, Modifier.weight(1f).padding(12.dp), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = model::clearError) { Text("关闭") }
                }
            }
            NavHost(navController = nav, startDestination = "keys", modifier = Modifier.weight(1f)) {
                composable("keys") { KeyList(vault!!.keys, model, { nav.navigate("key/${it.id}") }) }
                composable("notes") { NoteList(vault!!.notes, model, { nav.navigate("note/${it.id}") }) }
                composable("generator") { GeneratorScreen(model, { nav.navigate("key/${it.id}") }) }
                composable("settings") {
                    SettingsScreen(model, biometricHardwareAvailable,
                        onEnableBiometric, onDisableBiometric, onTheme, onAccent, themeMode, accentIndex,
                        onExport, onImport)
                }
                composable("key/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                    KeyEditor(model, vault!!.keys.firstOrNull { it.id == entry.arguments?.getString("id") }, { nav.popBackStack() })
                }
                composable("note/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                    NoteEditor(model, vault!!.notes.firstOrNull { it.id == entry.arguments?.getString("id") }, { nav.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun PageTitle(title: String, subtitle: String = "") {
    Column(Modifier.padding(horizontal = 22.dp, vertical = 16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = LocalAccent.current)
        if (subtitle.isNotEmpty()) Text(subtitle, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SearchBox(query: String, onQuery: (String) -> Unit) {
    OutlinedTextField(
        value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        placeholder = { Text("搜索名称、内容、标签或分类") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true, shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun Filters(tags: List<String>, categories: List<String>, selectedTag: String, selectedCategory: String,
                    onTag: (String) -> Unit, onCategory: (String) -> Unit) {
    if (tags.isEmpty() && categories.isEmpty()) return
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (selectedTag.isNotEmpty() || selectedCategory.isNotEmpty()) item {
            FilterChip(false, onClick = { onTag(""); onCategory("") }, label = { Text("全部") })
        }
        items(tags) { tag -> FilterChip(selectedTag == tag, onClick = { onTag(if (selectedTag == tag) "" else tag) }, label = { Text("# $tag") }) }
        items(categories) { category -> FilterChip(selectedCategory == category, onClick = { onCategory(if (selectedCategory == category) "" else category) }, label = { Text(category) }) }
    }
}

@Composable
private fun KeyList(list: List<KeyItem>, model: AppViewModel, onOpen: (KeyItem) -> Unit) {
    var query by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    val results = SearchFilter.keys(list, query, tag, category)
    Column {
        PageTitle("密钥", "安全保存每一个重要凭据")
        SearchBox(query) { query = it }
        Filters(list.flatMap { it.tags }.distinct(), list.map { it.category }.filter { it.isNotBlank() }.distinct(), tag, category,
            { tag = it }, { category = it })
        if (results.isEmpty()) EmptyState("暂无密钥，点击 + 新建")
        else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(results, key = { it.id }) { item ->
                GlassItem(item.name, item.note, item.category, item.tags, item.pinned, false,
                    onOpen = { onOpen(item) }, onPin = { model.toggleKeyPin(item) })
            }
        }
    }
}

@Composable
private fun NoteList(list: List<NoteItem>, model: AppViewModel, onOpen: (NoteItem) -> Unit) {
    var query by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }
    val results = SearchFilter.notes(list, query, tag, category, favoritesOnly)
    Column {
        PageTitle("笔记", "想法与秘密，都留在这里")
        SearchBox(query) { query = it }
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp)) {
            item { FilterChip(favoritesOnly, onClick = { favoritesOnly = !favoritesOnly }, label = { Text("★ 收藏") }) }
        }
        Filters(list.flatMap { it.tags }.distinct(), list.map { it.category }.filter { it.isNotBlank() }.distinct(), tag, category,
            { tag = it }, { category = it })
        if (results.isEmpty()) EmptyState("暂无笔记，点击 + 新建")
        else LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(results, key = { it.id }) { item ->
                GlassItem(item.title, item.body, item.category, item.tags, item.pinned, item.favorite,
                    onOpen = { onOpen(item) }, onPin = { model.toggleNotePin(item) }, onFavorite = { model.toggleFavorite(item) })
            }
        }
    }
}

@Composable
private fun GlassItem(title: String, subtitle: String, category: String, tags: List<String>, pinned: Boolean, favorite: Boolean,
                      onOpen: () -> Unit, onPin: () -> Unit, onFavorite: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().glass().clickable(onClick = onOpen).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = onPin) { Icon(if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, contentDescription = "置顶") }
            if (onFavorite != null) IconButton(onClick = onFavorite) {
                Icon(if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder, contentDescription = "收藏")
            }
        }
        if (subtitle.isNotBlank()) Text(subtitle, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
        if (category.isNotBlank() || tags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text((listOfNotNull(category.takeIf { it.isNotBlank() }) + tags.map { "#$it" }).joinToString("  "),
                color = cyanAccent, style = MaterialTheme.typography.labelMedium, maxLines = 2)
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(message, style = MaterialTheme.typography.bodyLarge) }
}

@Composable
fun SecretField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = modifier.fillMaxWidth(),
        label = { Text(label) }, singleLine = true,
        visualTransformation = if (visible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
        trailingIcon = { TextButton(onClick = { visible = !visible }) { Text(if (visible) "隐藏" else "显示") } })
}

@Composable
private fun UnlockScreen(firstRun: Boolean, busy: Boolean, error: String, biometricAvailable: Boolean,
                         onBiometric: () -> Unit, onUnlock: (CharArray, CharArray?, (() -> Unit)?) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth().padding(28.dp).glass().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.background(Brush.horizontalGradient(listOf(LocalAccent.current, cyanAccent)), RoundedCornerShape(20.dp)).padding(horizontal = 24.dp, vertical = 8.dp)) {
                Text("◇", style = MaterialTheme.typography.displayLarge, color = androidx.compose.ui.graphics.Color.White)
            }
            Text("KeyVault", style = MaterialTheme.typography.headlineLarge)
            Text(if (firstRun) "设置主密码，开启私人保险库" else "欢迎回来，解锁你的保险库")
            SecretField(password, { password = it }, "主密码")
            if (firstRun) SecretField(confirm, { confirm = it }, "确认主密码")
            if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
            Button(onClick = {
                onUnlock(password.toCharArray(), if (firstRun) confirm.toCharArray() else null) {
                    password = ""; confirm = ""
                }
            }, enabled = !busy && password.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text(if (busy) "请稍候…" else if (firstRun) "创建保险库" else "解锁")
            }
            if (biometricAvailable && !firstRun) TextButton(onClick = onBiometric) { Text("使用生物识别") }
        }
    }
}
