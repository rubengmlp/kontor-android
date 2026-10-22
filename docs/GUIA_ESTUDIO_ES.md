# Guía de estudio de Kontor

Esta guía explica el proyecto **Kontor** en español y asume que nunca trabajaste con Kotlin ni con Jetpack Compose. No intenta reemplazar la documentación oficial: intenta darte el mapa mental mínimo para **entender el código, defenderlo en una entrevista y poder modificarlo**.

> Kontor es un proyecto de portafolio independiente. No tiene relación con DKB. Todas las cuentas, IBAN y movimientos son ficticios.

---

## 0. Cómo usar esta guía

Tenés dos objetivos distintos:

1. **Entender la app**: qué hace cada capa y por qué está separada.
2. **Poder contarlo**: explicar decisiones, trade-offs y qué harías distinto en producción.

Recorrido recomendado:

- **Primera pasada (45 min)**: secciones 1 a 5. Abrí el proyecto en Android Studio y seguí el flujo de una pantalla.
- **Segunda pasada (60 min)**: secciones 6 a 9. Mirá Hilt, Room, Retrofit y tests con el código al lado.
- **Tercera pasada (30 min)**: secciones 10 a 13. Practicá el guion de demo y las preguntas.
- **Antes de la entrevista (15 min)**: repasá la tabla de “preguntas probables” y el guion de 6 minutos.

Comandos útiles:

```bash
./gradlew testDebugUnitTest          # tests JVM de los módulos Android
./gradlew :core:domain:test          # tests puros de dominio
./gradlew lintDebug assembleRelease  # lint + build release con R8
./gradlew :app:connectedDebugAndroidTest # tests de UI (emulador encendido)
```

PIN de demo: **1234**. Application id de debug: `com.rubengmlp.kontor.debug`.

---

## 1. Resumen en 60 segundos

Kontor es una app de banca para pequeñas empresas. Muestra cuentas, movimientos y permite hacer una transferencia completa:

```text
Unlock → Home → Activity → Transaction detail
                   └── Transfer → Review → Success → Home actualizado
```

La app demuestra:

- Kotlin moderno y Jetpack Compose con Material 3.
- Arquitectura multi-módulo con dependencias hacia adentro.
- MVVM y flujo de datos unidireccional (UDF): el estado baja, los eventos suben.
- Coroutines y Flow: el UI observa `StateFlow`; Room es la única fuente de verdad.
- Hilt para inyección de dependencias.
- Room para persistencia y Retrofit/OkHttp para red.
- Un backend simulado con un `Interceptor` de OkHttp, para que la demo sea autocontenida.
- Validación de IBAN (ISO 13616) y de transferencias en el dominio.
- Tests unitarios de dominio y ViewModels, más tests de UI con Compose.
- CI con GitHub Actions y build release con R8.

La frase para la entrevista:

> “Es una demo de banca pequeña con Clean Architecture pragmática: dominio puro y testeable, datos con Room como fuente de verdad y Retrofit detrás de una interfaz, y UI Compose con ViewModels que exponen un único `UiState`. La transferencia es atómica en Room y el backend está simulado con un interceptor para que se pueda ejecutar sin infraestructura.”

---

## 2. Arquitectura

### 2.1 Regla de dependencias

```text
:app
 ├── :feature:accounts ─┐
 ├── :feature:transfer ─┼──> :core:designsystem
 ├── :feature:profile ──┘
 ├── :core:data ───────────> :core:domain
 └── :core:designsystem
```

La regla es simple: **las dependencias apuntan hacia el dominio**. `:core:domain` no conoce Android, Room, Retrofit ni Compose. Eso permite testear las reglas de negocio con JUnit puro, sin emulador.

- `:core:domain`: modelos (`Account`, `Transaction`), contratos (`BankRepository`), dinero (`Money`), validaciones (`IbanValidator`, `TransferValidator`).
- `:core:data`: implementaciones de los contratos. Traduce entre el mundo externo (DTOs de Retrofit/entidades de Room) y los modelos de dominio.
- `:core:designsystem`: tema, tipografía, colores, componentes reutilizables. No conoce features.
- `:feature:*`: una pantalla (o un flujo) y su ViewModel. No dependen entre sí.
- `:app`: composición de todo: `MainActivity`, navegación, Hilt, unlock.

### 2.2 ¿Por qué multi-módulo?

No es solo estética. Obliga a que las dependencias sean explícitas: si un feature intenta usar una entidad de Room, no compila porque no depende de `:core:data`. También acelera builds incrementales y permite que los tests de dominio corran sin Android.

Para una app de este tamaño, un solo módulo también sería razonable. La decisión multi-módulo es **demostrativa y defendible**: muestra límites claros y facilita explicar la arquitectura.

### 2.3 Flujo de una pantalla

```text
Room Flow ─┐
Room Flow ─┼─> combine(...) ─> AccountsUiState ─> StateFlow ─> Compose
DataStore ─┤                                                │
UI state ──┘                                                ▼
                                             eventos: onRefresh(), toggle...
```

1. El ViewModel combina flujos del repositorio y estado local.
2. Produce un objeto inmutable `UiState`.
3. Compose lo observa con `collectAsStateWithLifecycle()`.
4. La UI no modifica el estado: llama funciones del ViewModel.
5. El ViewModel actualiza el repositorio o su estado local.
6. Room emite de nuevo y se recompone la pantalla.

---

## 3. Kotlin mínimo que aparece en el proyecto

### 3.1 `val`, `var` e inferencia

```kotlin
val account = accountDao.getById(id)   // referencia inmutable
var pin = ""                           // variable local mutable
```

`val` no significa que el objeto sea inmutable: significa que la referencia no se reasigna. Preferí `val` salvo que necesites reasignar.

### 3.2 Null safety

Kotlin distingue `String` de `String?`.

```kotlin
val name: String? = transaction.counterparty
val shown = name ?: "—"                 // Elvis: si es null, usa "—"
val upper = name?.uppercase()            // safe call: null si name es null
```

En el proyecto:

```kotlin
val formatted = receipt?.amountCents ?: state.amountCents ?: 0L
```

Se lee: “si hay recibo usá su monto; si no, el monto del formulario; si tampoco, cero”.

### 3.3 `data class`

```kotlin
data class Account(
    val id: String,
    val name: String,
    val balanceCents: Long,
    val availableCents: Long,
    val type: AccountType,
)
```

Una `data class` genera `equals`, `hashCode`, `toString`, `copy` y `componentN`. `copy` es clave para estados inmutables:

```kotlin
_state.update { current ->
    current.copy(isSubmitting = true, submitError = null)
}
```

No mutamos el estado anterior: creamos uno nuevo. Eso hace que Compose detecte el cambio y que los tests sean predecibles.

### 3.4 `enum class`

```kotlin
enum class AccountType { GIRO, SAVINGS, CREDIT }
enum class TransferStep { FORM, REVIEW, SUCCESS }
```

Los enums son seguros para `when`: si agregás un caso y el `when` es exhaustivo, el compilador te obliga a cubrirlo.

### 3.5 `object`

```kotlin
object Money { ... }
object IbanValidator { ... }
```

`object` es un singleton. `Money` no tiene estado, solo funciones puras. Es fácil de testear y de usar desde cualquier capa.

### 3.6 Funciones de extensión

```kotlin
fun Instant.asShortDate(): String = ...
fun TransactionCategory.label(): String = when (this) { ... }
```

Permiten agregar comportamiento legible sin heredar:

```kotlin
transaction.bookedAt.asShortDate()
```

Se usan en `TransactionRow.kt` para fechas y etiquetas de categoría.

### 3.7 `when` como expresión

```kotlin
val value = when (character) {
    in '0'..'9' -> character - '0'
    in 'A'..'Z' -> character - 'A' + 10
    else -> return false
}
```

En Kotlin `when` devuelve un valor. Es más expresivo que una cadena de `if/else`.

### 3.8 Lambdas, `let`, `apply`, `use`

```kotlin
localById[remote.id]?.let { local ->
    remote.copy(balanceCents = local.balanceCents)
}
```

`let` ejecuta un bloque solo si el receptor no es null.

```kotlin
NumberFormat.getCurrencyInstance(locale).apply {
    currency = Currency.getInstance("EUR")
}.format(...)
```

`apply` configura un objeto y devuelve el mismo objeto. Se usa en `Money.format` y en la configuración de OkHttp.

```kotlin
context.assets.open(path).bufferedReader().use { it.readText() }
```

`use` cierra el recurso aunque ocurra una excepción (`AutoCloseable`).

### 3.9 Colecciones

```kotlin
val total = accounts.sumOf { it.balanceCents }
val byId = accounts.associateBy { it.id }
val visible = transactions
    .filter { it.title.contains(query, ignoreCase = true) }
    .filter { it.amountCents < 0 }
```

`map`, `filter`, `sumOf`, `associateBy`, `firstOrNull` aparecen en ViewModels y repositorios. Son operaciones de colecciones inmutables: devuelven una lista nueva.

### 3.10 `Result`

La capa de datos devuelve `Result<T>`:

```kotlin
val result = repository.refresh()
result.exceptionOrNull()?.let { error -> ... }
```

Y al consumir una transferencia:

```kotlin
result.fold(
    onSuccess = { receipt -> ... },
    onFailure = { error -> ... },
)
```

`Result` hace explícito que una operación puede fallar. No reemplaza el manejo de errores de dominio, pero evita excepciones no controladas cruzando capas.

### 3.11 Anotaciones y procesamiento

- `@Serializable` genera serializadores de kotlinx.serialization.
- `@Entity`, `@Dao`, `@Query` generan código de Room.
- `@HiltViewModel`, `@Inject`, `@Module` generan el grafo de Hilt.
- KSP procesa esas anotaciones en compilación.

No hay reflexión en runtime para estas piezas: el código generado se compila dentro del APK.

---

## 4. Jetpack Compose

### 4.1 El modelo mental

Compose no usa XML ni muta vistas. Escribís funciones que describen la UI:

```kotlin
@Composable
fun Greeting(name: String) {
    Text("Hola, $name")
}
```

Cuando el estado que lee una función cambia, Compose vuelve a ejecutar esa función (**recomposición**). No se recrean objetos de vista: se actualiza el árbol de composición.

### 4.2 Estado y recomposición

```kotlin
var pin by rememberSaveable { mutableStateOf("") }
```

- `mutableStateOf` crea un estado observable.
- `by` delega `getValue`/`setValue`.
- `remember` conserva el valor entre recomposiciones.
- `rememberSaveable` además lo guarda en el `SavedStateHandle` (rotación, recreación).
- Leer `pin` dentro de un `@Composable` crea una suscripción: cuando cambia, se recompone.

En producción, un PIN no debería persistirse en `SavedState`; acá se usa para que la demo sobreviva a una rotación.

### 4.3 State hoisting y separación Route/Screen

En Kontor cada feature tiene dos capas:

```kotlin
@Composable
fun TransferRoute(...) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TransferScreen(state = state, onContinue = viewModel::onContinue, ...)
}

@Composable
fun TransferScreen(
    state: TransferUiState,
    onContinue: () -> Unit,
    ...
) { ... }
```

- `Route`: conoce el ViewModel y la navegación.
- `Screen`: recibe estado y callbacks, no sabe de Hilt ni de Navigation.

Eso permite testear la pantalla con estados falsos y reutilizarla en previews.

### 4.4 `collectAsStateWithLifecycle`

```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()
```

Es la forma recomendada de observar un `Flow` desde Compose: pausa la colección cuando la pantalla no está visible (`STARTED`) y la reanuda al volver. Ahorra recursos y evita actualizaciones en background.

Comparalo con `collectAsState()`: este último no conoce el ciclo de vida.

### 4.5 Listas perezosas

```kotlin
LazyColumn {
    items(state.accounts, key = { it.id }) { account ->
        AccountCard(account)
    }
}
```

`LazyColumn` solo compone los ítems visibles. `key` le dice a Compose cómo identificar cada elemento; sin claves, al reordenar o insertar se pueden reutilizar mal los estados internos.

En Activity se agrupan movimientos por día:

```kotlin
item(key = "header_$date") { DayHeader(date) }
items(transactions, key = { it.id }) { transaction -> ... }
```

### 4.6 Modifiers

Un `Modifier` es una cadena de decoraciones. **El orden importa**:

```kotlin
Modifier
    .fillMaxSize()
    .background(gradient)
    .statusBarsPadding()
```

Primero ocupa toda la pantalla, luego pinta el fondo, luego agrega padding interno para no quedar debajo de la barra de estado. Si invertís `background` y `padding`, el fondo no cubre el área del padding.

En el home, el `LazyColumn` tiene:

```kotlin
modifier = Modifier
    .fillMaxSize()
    .statusBarsPadding()
```

La barra superior compartida (`KontorTopBar`) ya aplica `statusBarsPadding`; el home no usa top bar, por eso lo aplica su contenedor. Si lo aplicáramos dos veces, el contenido quedaría demasiado abajo.

### 4.7 Material 3 y tema

`KontorTheme` envuelve `MaterialTheme` con:

- `colorScheme`: colores semánticos (`primary`, `surface`, `error`, etc.).
- `typography`: serif para display, sans para datos densos.
- `shapes`: esquinas redondeadas.
- `LocalKontorExtraColors`: colores propios de marca (`positive`, `negative`, `heroStart`, etc.) vía `CompositionLocal`.

Los features piden colores al tema, no hardcodean hexadecimales. Eso permite que dark mode funcione sin duplicar pantallas.

### 4.8 Insets y edge-to-edge

La app dibuja detrás de las barras del sistema (`enableEdgeToEdge`). Hay que respetar los insets:

- `statusBarsPadding()`: evita la barra de estado.
- `navigationBarsPadding()`: evita la barra de navegación/gestos.
- `Scaffold` entrega `innerPadding` que incluye la bottom bar.
- `WindowInsets(0, 0, 0, 0)` en `KontorApp` desactiva los insets automáticos del Scaffold porque la app los maneja explícitamente.

El `Scaffold` de `KontorApp` padea el `NavHost` con `innerPadding` para que el contenido no quede debajo de la bottom navigation. La `NavigationBar` de Material 3 maneja el inset inferior por su cuenta.

### 4.9 Accesibilidad y test tags

- `contentDescription` en iconos interactivos (por ejemplo, el ojo para ocultar saldos).
- Targets táctiles de al menos 48 dp.
- `testTag("pin_key_1")` permite que los tests de Compose encuentren botones por semántica, no por coordenadas.

### 4.10 Errores comunes al leer Compose

- **No** llames funciones `suspend` directamente desde un `@Composable`; usá `LaunchedEffect` o un ViewModel.
- **No** guardes lógica de negocio en la pantalla; va al ViewModel/dominio.
- **No** uses `collectAsState()` si podés usar `collectAsStateWithLifecycle()`.
- **No** uses índices como `key` en listas si existe un id estable.
- **Sí** hacé que la pantalla sea una función del estado: mismo estado, misma UI.

---

## 5. MVVM, coroutines y Flow

### 5.1 El ViewModel

Un ViewModel sobrevive a cambios de configuración y expone estado. En Kontor:

```kotlin
@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: BankRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() { ... }
```

No conoce Compose. No conoce Android Views. Solo conoce dominio y coroutines.

### 5.2 `StateFlow` y `MutableStateFlow`

- `MutableStateFlow` es la versión mutable, privada.
- `StateFlow` es la versión pública, de solo lectura.
- `StateFlow` siempre tiene un valor actual; es ideal para UI.
- `SharedFlow` se usa para eventos únicos (navegación, snackbars) y no tiene valor inicial.
- `StateFlow` conflaciona: si el consumidor es lento, se queda con el último valor.

En `TransferViewModel`:

```kotlin
private val _state = MutableStateFlow(TransferUiState())
val state = _state.asStateFlow()
```

En `AccountsViewModel` no hay `MutableStateFlow` para el estado completo: se construye con `combine` y `stateIn`.

### 5.3 `combine`

```kotlin
val state = combine(
    repository.observeAccounts(),
    repository.observeRecentTransactions(6),
    preferencesRepository.preferences,
    local,
) { accounts, transactions, preferences, localState ->
    AccountsUiState(...)
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = AccountsUiState(),
)
```

`combine` espera a que todos los flujos tengan un valor y emite una nueva UI cuando cualquiera cambia. `local` es estado propio del ViewModel: loading, refreshing, error.

### 5.4 `stateIn` y `SharingStarted`

`stateIn` convierte un `Flow` frío en un `StateFlow` caliente, con scope y valor inicial:

- `WhileSubscribed(5_000)`: empieza a recolectar cuando hay suscriptores y espera 5 segundos después del último antes de cancelar. Así una rotación no reinicia la carga.
- `Eagerly`: empieza ya, sin importar suscriptores.
- `Lazily`: empieza con el primer suscriptor y no se detiene.

### 5.5 Coroutines

```kotlin
init { refresh() }

fun refresh() {
    if (local.value.isRefreshing) return
    viewModelScope.launch {
        local.update { it.copy(isRefreshing = true, error = null) }
        val result = repository.refresh()
        local.update { it.copy(isRefreshing = false, ...) }
    }
}
```

- `viewModelScope` cancela el trabajo cuando el ViewModel se destruye.
- `launch` no bloquea el hilo principal.
- `suspend` indica que una función puede suspenderse sin bloquear.
- `Dispatchers.Main` es el default de `viewModelScope`; el trabajo de IO se hace en las capas de datos (Retrofit/Room ya usan sus dispatchers).

En `DefaultBankRepository.refresh()` se ejecutan dos llamadas en paralelo:

```kotlin
coroutineScope {
    val accounts = async { api.getAccounts() }
    val transactions = async { api.getTransactions() }
    ...
}
```

`async` devuelve un `Deferred`; `await()` espera el resultado. Si una falla, `coroutineScope` cancela la otra.

### 5.6 Cancelación

`resultOf` vuelve a lanzar `CancellationException`:

```kotlin
private suspend fun <T> resultOf(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: Exception) {
    Result.failure(error)
}
```

Nunca convertís una cancelación en un error de negocio: si el usuario sale de la pantalla, no querés mostrar “falló la transferencia”.

### 5.7 State machine en Transfer

`TransferStep` es una máquina de estados explícita:

```text
FORM --onContinue válido--> REVIEW --onConfirm OK--> SUCCESS
  ^                            |
  └──────── onEdit ────────────┘
```

- `onContinue`: valida y, si está bien, pasa a REVIEW.
- `onConfirm`: llama al repositorio; si falla, queda en REVIEW con `submitError`.
- `startNewTransferIfComplete`: si el usuario vuelve a la pestaña después de una transferencia exitosa, limpia el recibo y vuelve a FORM. Los borradores se conservan.
- `TransferScreen` muestra un estado u otro con `when (state.step)`.

Esta separación evita booleanos sueltos (`isReview`, `isSuccess`, `isLoading`) que se contradicen entre sí.

---

## 6. Hilt

Hilt es una capa sobre Dagger. Genera el grafo de dependencias en compilación.

### 6.1 Puntos de entrada

```kotlin
@HiltAndroidApp
class KontorApplication : Application()
```

`@AndroidEntryPoint` en `MainActivity` permite inyectar dependencias en la Activity y en los ViewModels obtenidos por Compose.

```kotlin
@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: BankRepository,
    ...
) : ViewModel()
```

`hiltViewModel()` en el Route crea/obtiene el ViewModel con el scope correcto.

### 6.2 Módulos

`DatabaseModule` provee la base Room y los DAOs:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KontorDatabase =
        Room.databaseBuilder(context, KontorDatabase::class.java, "kontor.db").build()
}
```

`NetworkModule` provee `Json`, `OkHttpClient`, `Retrofit` y `BankApi`.

`RepositoryModule` usa `@Binds` para conectar interfaces con implementaciones:

```kotlin
@Binds
@Singleton
abstract fun bindBankRepository(implementation: DefaultBankRepository): BankRepository
```

Así los ViewModels dependen de `BankRepository`, no de `DefaultBankRepository`.

### 6.3 Scopes

- `@Singleton` en `SingletonComponent`: una instancia por proceso (DB, OkHttp, repositorios).
- `@HiltViewModel`: una instancia por ViewModelStoreOwner (pantalla/back stack).
- `@ApplicationContext`: contexto de aplicación, seguro para singletons. Nunca inyectes una Activity en un singleton.

### 6.4 Por qué Hilt y no un Service Locator

Hilt valida el grafo en compilación: si falta un binding, el build falla. También hace explícitas las dependencias en el constructor, lo que mejora testeabilidad. El costo es más anotaciones y un build ligeramente más lento.

---

## 7. Room

### 7.1 Entidades

Las entidades son tablas:

```kotlin
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iban: String,
    val balanceCents: Long,
    val availableCents: Long,
    val type: String,
)
```

Los enums se guardan como `String` (`type.name`). Al leerlos se convierten con `AccountType.entries.firstOrNull { it.name == type } ?: GIRO`. Eso desacopla el esquema de los cambios internos del enum.

### 7.2 DAOs y Flow

```kotlin
@Query("SELECT * FROM accounts ORDER BY ...")
fun observeAll(): Flow<List<AccountEntity>>
```

Un `Flow` de Room emite cada vez que cambia una tabla observada. No hay que refrescar manualmente después de una escritura.

### 7.3 `@Upsert`

```kotlin
@Upsert
suspend fun upsertAll(accounts: List<AccountEntity>)
```

Inserta o actualiza por clave primaria. Se usa en el refresh para sincronizar el seed sin duplicar filas.

### 7.4 Atomicidad de la transferencia

```kotlin
@Transaction
open suspend fun applyTransfer(
    accountId: String,
    balanceCents: Long,
    availableCents: Long,
    transaction: TransactionEntity,
) {
    updateBalance(accountId, balanceCents, availableCents)
    insertTransaction(transaction)
}
```

Si falla la inserción, Room revierte el update. El saldo y el movimiento no pueden quedar inconsistentes.

### 7.5 Esquema exportado

`core/data/schemas/.../1.json` es el esquema de la versión 1. En producción se versiona y se escriben migraciones (`Migration(1, 2)`) en vez de usar `fallbackToDestructiveMigration`, que borra datos.

### 7.6 Lo que falta para producción

- Foreign key de `transactions.accountId` a `accounts.id`.
- Índices por `accountId`/`bookedAt` si la tabla crece.
- Migraciones probadas.
- Encriptación (SQLCipher) o minimización de PII.
- Estrategia de retención y borrado.

---

## 8. Retrofit, OkHttp y serialización

### 8.1 La interfaz

```kotlin
interface BankApi {
    @GET("v1/accounts") suspend fun getAccounts(): List<AccountDto>
    @GET("v1/transactions") suspend fun getTransactions(): List<TransactionDto>
    @POST("v1/transfers") suspend fun postTransfer(@Body request: TransferRequestDto): TransferReceiptDto
}
```

Retrofit genera la implementación en runtime. Las funciones `suspend` se ejecutan fuera del hilo principal.

### 8.2 DTOs vs modelos de dominio

- `AccountDto` representa el JSON del backend.
- `Account` es el modelo que usa la app.
- `toEntity()` y `toDomain()` traducen entre capas.

Esto evita que un cambio del backend rompa la UI y que la UI dependa de nombres de campos JSON.

### 8.3 `kotlinx.serialization`

```kotlin
@Serializable
data class AccountDto(...)
```

El plugin de serialización genera el parser en compilación. `Json { ignoreUnknownKeys = true }` permite que el backend agregue campos sin romper el cliente.

### 8.4 Interceptores

```kotlin
OkHttpClient.Builder()
    .addInterceptor(authInterceptor)
    .addInterceptor(loggingInterceptor)
    .addInterceptor(demoApiInterceptor)
```

- `AuthInterceptor`: agrega `Authorization: Bearer demo-session-token` y `Accept`.
- `HttpLoggingInterceptor`: loguea método/URL/status; redacta el header Authorization.
- `DemoApiInterceptor`: responde con assets locales y 320 ms de latencia simulada.

En producción el token vendría de un almacén seguro y se refrescaría con un `Authenticator` al recibir 401.

### 8.5 El backend simulado

```kotlin
val path = request.url.encodedPath
Thread.sleep(320)
val (code, body) = when {
    path.endsWith("/accounts") -> 200 to readAsset("demo/accounts.json")
    path.endsWith("/transactions") -> 200 to readAsset("demo/transactions.json")
    path.endsWith("/transfers") && request.method == "POST" -> 200 to transferReceipt()
    else -> 404 to """{"message":"Not found"}"""
}
```

Se usa el stack real de Retrofit/OkHttp: URL, request, interceptores, converter JSON y respuesta HTTP. Solo se reemplaza el servidor. Para conectar un backend real se quita este interceptor y se cambia `baseUrl`.

### 8.6 Errores y `Result`

`DefaultBankRepository` envuelve las llamadas en `resultOf`:

```kotlin
override suspend fun submitTransfer(request: TransferRequest): Result<TransferReceipt> = resultOf {
    ...
}
```

Los ViewModels deciden cómo mostrar el error. En una app real distinguirías errores de red, validación, autenticación y conflicto de negocio (por ejemplo, 409 por idempotencia).

---

## 9. Tests

### 9.1 Dominio puro

`MoneyTest`, `IbanValidatorTest` y `TransferValidatorTest` no necesitan Android. Prueban:

- formato de centavos a euros;
- parseo de `1.234,56` y `1234.56`;
- entradas inválidas;
- IBAN válido, minúsculas y checksum roto;
- transferencia válida, destinatario vacío, IBAN inválido, monto cero/negativo, saldo insuficiente y referencia larga.

### 9.2 ViewModels con fakes

No se usa MockK. Se escriben fakes explícitos:

```kotlin
private class FakeBankRepository : BankRepository {
    val accountsFlow = MutableStateFlow(...)
    override fun observeAccounts(): Flow<List<Account>> = accountsFlow
    ...
}
```

Ventajas: el test es legible y no depende de detalles de una librería de mocking. Desventaja: hay que mantener los fakes cuando cambia la interfaz (algo bueno: te obliga a pensar el contrato).

### 9.3 Coroutines de test

```kotlin
private val dispatcher = StandardTestDispatcher()

@Before fun setUp() { Dispatchers.setMain(dispatcher) }
@After fun tearDown() { Dispatchers.resetMain() }
```

`StandardTestDispatcher` permite controlar el tiempo y ejecutar tareas con `advanceUntilIdle()`. Sin esto, los tests de `viewModelScope` serían no deterministas.

### 9.4 Turbine

Turbine permite consumir un `Flow` como una secuencia de eventos:

```kotlin
viewModel.state.test {
    awaitItem()
    advanceUntilIdle()
    val state = expectMostRecentItem()
    assertEquals(3, state.recentTransactions.size)
}
```

Así se verifica el estado inicial y el estado final sin `Thread.sleep`.

### 9.5 Compose UI tests

`UnlockScreenTest` monta la pantalla con `createComposeRule()` y usa semántica:

```kotlin
composeRule.onNodeWithTag("pin_key_1").performClick()
composeRule.waitUntil { unlocked }
```

Prueba dos caminos: PIN correcto desbloquea y PIN incorrecto muestra error sin desbloquear.

### 9.6 Limitación conocida de lint

`:app` desactiva `lint.checkTestSources` por un crash de AGP/Kotlin 2.2 al resolver símbolos de Compose UI test. Los tests de UI siguen ejecutándose con `connectedDebugAndroidTest`. Es una limitación de tooling documentada, no una excusa para no testear.

---

## 10. Dominio bancario: decisiones que importan

### 10.1 Dinero en centavos

Nunca uses `Float`/`Double` para dinero. `0.1 + 0.2` no es exactamente `0.3` en binario. Kontor usa `Long` de centavos y `BigDecimal` solo para parsear/formatear.

```text
€24.835,00  ->  2_483_500L
−€1.240,50  ->  −124_050L
```

### 10.2 Balance vs disponible

- **Balance**: lo que hay contablemente.
- **Disponible**: lo que podés usar ahora, después de retenciones, límites y operaciones pendientes.

El giro tiene €500 retenidos por una tarjeta: balance €24.835, disponible €24.335. La validación usa disponible. Es una distinción que muchos demos ignoran y que en banca es central.

### 10.3 IBAN

`IbanValidator`:

1. Normaliza: mayúsculas, sin espacios ni guiones.
2. Verifica longitud 15–34 y forma `[A-Z]{2}\d{2}[A-Z0-9]+`.
3. Mueve los primeros 4 caracteres al final.
4. Convierte letras a números (`A=10`, ..., `Z=35`).
5. Calcula módulo 97 y exige resto 1.

No alcanza con una regex: el checksum detecta errores de tipeo.

### 10.4 Atomicidad

La transferencia actualiza saldo e inserta el movimiento en una transacción de Room. En un backend real, además, el servidor debe ser la autoridad y usar una clave de idempotencia para que un reintento no mueva el dinero dos veces.

### 10.5 Idempotencia

Una red puede fallar después de que el servidor procesó la transferencia. Si el cliente reintenta sin idempotency key, puede duplicar el débito. En producción:

```http
POST /v1/transfers
Idempotency-Key: 7f3c...
```

El servidor guarda la clave y devuelve el mismo recibo si se repite.

### 10.6 SCA, biometría y seguridad

La biometría de la demo es un **gate de presentación**, no una firma criptográfica. En una app real:

- La sesión se vincula a una clave del Keystore.
- La biometría desbloquea una clave privada, no solo una pantalla.
- Los tokens se guardan cifrados y se refrescan.
- Se aplica `FLAG_SECURE` para capturas/recientes.
- Hay certificate pinning y monitoreo de integridad.
- Las operaciones sensibles requieren SCA/PSD2 y límites/fraude.

### 10.7 Privacidad en UI

Ocultar saldos no es cosmetico: evita hombro-hackers. En producción también:

- Enmascarar IBAN en listas.
- No loguear montos/PII.
- No incluir datos sensibles en analytics.
- Limpiar notificaciones y previews.

---

## 11. Recorrido por archivos

### `:core:domain`

| Archivo | Qué mirar |
| --- | --- |
| `model/Account.kt` | `Account`, `AccountType`, `maskedIban` |
| `model/Transaction.kt` | modelo, categorías, estados |
| `model/Transfer.kt` | request, receipt, `TransferValidation` |
| `money/Money.kt` | formato, parseo, sanitización |
| `validation/IbanValidator.kt` | mod-97 |
| `validation/TransferValidator.kt` | reglas de negocio |
| `repository/BankRepository.kt` | contrato de datos |

### `:core:data`

| Archivo | Qué mirar |
| --- | --- |
| `local/AccountEntity.kt` | mapeo Room ↔ dominio |
| `local/TransactionEntity.kt` | fechas como epoch millis |
| `local/AccountDao.kt` | consultas y `Flow` |
| `local/TransactionDao.kt` | orden y límite |
| `local/TransferDao.kt` | `@Transaction` atómica |
| `local/DatabaseModule.kt` | provisión de Room |
| `remote/BankApi.kt` | endpoints |
| `remote/BankDtos.kt` | DTOs serializables |
| `remote/NetworkModule.kt` | OkHttp/Retrofit/JSON |
| `remote/DemoApiInterceptor.kt` | backend hermético |
| `repository/DefaultBankRepository.kt` | refresh, merge y submit |
| `repository/DataStoreUserPreferencesRepository.kt` | preferencias |
| `repository/RepositoryModule.kt` | `@Binds` |

### `:feature:accounts`

| Archivo | Qué mirar |
| --- | --- |
| `AccountsViewModel.kt` | `combine`, totales, refresh, hide |
| `AccountsScreen.kt` | home, hero, carrusel, recientes |
| `TransactionsViewModel.kt` | búsqueda/filtros derivados |
| `TransactionsScreen.kt` | agrupación por día |
| `TransactionDetailViewModel.kt` | `combine` de transacción + cuenta |
| `TransactionDetailScreen.kt` | detalle |
| `TransactionRow.kt` | extensiones de fecha/categoría |
| tests | fakes + Turbine |

### `:feature:transfer`

| Archivo | Qué mirar |
| --- | --- |
| `TransferViewModel.kt` | máquina de estados, validación, submit, reset |
| `TransferScreen.kt` | form, review, success, progreso |
| tests | éxito, error, cambio de cuenta, reset |

### `:feature:profile`

| Archivo | Qué mirar |
| --- | --- |
| `ProfileViewModel.kt` | preferencias |
| `ProfileScreen.kt` | tema, biometría, hide, lock, about |
| `AboutScreen.kt` | arquitectura contada dentro de la app |

### `:app`

| Archivo | Qué mirar |
| --- | --- |
| `MainActivity.kt` | edge-to-edge, tema, unlock, status bars |
| `KontorApp.kt` | Scaffold, bottom nav, typed routes |
| `navigation/Routes.kt` | rutas `@Serializable` |
| `ui/UnlockScreen.kt` | PIN, teclado, biometría |
| `ui/BiometricUnlock.kt` | wrapper de `BiometricPrompt` |
| `AppViewModel.kt` | preferencias y estado unlocked |
| `KontorApplication.kt` | `@HiltAndroidApp` |

---

## 12. Guion de demo (6 minutos)

1. **Unlock (30 s)** — “La app arranca bloqueada. Uso PIN 1234; si el dispositivo tiene biometría enrolada, aparece el prompt real.”
2. **Home (60 s)** — “Balance total vs disponible. El giro tiene una retención de €500; la tarjeta aporta línea de crédito. El ojo oculta saldos y persiste.”
3. **Activity (60 s)** — “Busco Studio Nord, filtro entradas, abro el detalle. Los datos vienen de Room, no de la red.”
4. **Transfer (90 s)** — “Elijo Anna Weber, monto 250,00. El IBAN se valida con mod-97 y el monto contra el disponible. Reviso y confirmo. Room actualiza saldo y movimiento en una transacción.”
5. **Profile (45 s)** — “Cambio a dark, abro ‘How this app is built’, bloqueo.”
6. **Código (90 s)** — Mostrá `TransferViewModel.onConfirm`, `DefaultBankRepository.submitTransfer`, `TransferDao.applyTransfer` y un test. “La lógica está en dominio; el ViewModel orquesta; la UI solo dibuja estado.”

Frase de cierre:

> “Lo que más me interesa mostrar no es la pantalla: es que el flujo es consistente, testeable y fácil de conectar a un backend real. El interceptor de demo se reemplaza por una URL y el resto del camino ya es Retrofit/OkHttp.”

---

## 13. Preguntas probables (con respuestas cortas)

### Arquitectura

**1. ¿Por qué multi-módulo?**
Para imponer límites de dependencias, aislar dominio puro, acelerar builds incrementales y permitir tests sin Android. En una app chica también sería válido un módulo; acá es una decisión demostrativa.

**2. ¿Qué capa conoce a quién?**
`app` conoce features y data; features conocen domain/designsystem; data conoce domain; domain no conoce a nadie. La UI nunca usa DTOs ni entidades de Room.

**3. ¿Dónde está la lógica de negocio?**
En `:core:domain`: validación de IBAN, reglas de transferencia, formato de dinero. Los ViewModels orquestan; no implementan las reglas.

**4. ¿Por qué no usar un solo `UiState` global?**
Porque cada pantalla tiene su estado y su ciclo de vida. Un estado global genera acoplamiento y recomposiciones innecesarias.

**5. ¿Cómo agregarías una pantalla?**
Nuevo `@Serializable` route, `composable<Route>` en `KontorApp`, feature con `Route` + `Screen` + `ViewModel` + tests. Si necesita datos nuevos, se agrega al contrato de dominio y se implementa en data.

### Compose

**6. ¿Qué es recomposición?**
Volver a ejecutar funciones `@Composable` cuyas lecturas de estado cambiaron. Compose actualiza solo las partes afectadas.

**7. ¿Diferencia entre `remember` y `rememberSaveable`?**
`remember` sobrevive recomposiciones; `rememberSaveable` también sobrevive recreación de Activity/rotación guardando en `SavedStateHandle`.

**8. ¿Por qué `collectAsStateWithLifecycle`?**
Porque pausa la colección cuando la pantalla no está visible, respetando el ciclo de vida y ahorrando trabajo.

**9. ¿Qué hace `LaunchedEffect(pin)`?**
Ejecuta el bloque cuando cambia `pin` y cancela el anterior. Se usa para validar el PIN al llegar a 4 dígitos.

**10. ¿Por qué `key` en `LazyColumn`?**
Para que Compose identifique ítems por id y no reutilice estado incorrectamente al insertar/reordenar.

**11. ¿Cómo manejás insets?**
Edge-to-edge + `statusBarsPadding`, `navigationBarsPadding`, `Scaffold.innerPadding`. La top bar compartida padea la barra de estado; el home lo hace en su lista.

**12. ¿Cómo evitás que la UI tenga lógica?**
La pantalla recibe `UiState` y callbacks. La lógica de validación/estado vive en ViewModel/dominio. `Route` conecta ViewModel; `Screen` es stateless.

### Kotlin/coroutines

**13. ¿Por qué `StateFlow` y no `LiveData`?**
Es Kotlin-first, funciona con coroutines, tiene valor inicial, conflaciona y se testea sin Android.

**14. ¿Diferencia `StateFlow` vs `SharedFlow`?**
`StateFlow` siempre tiene un valor y es un estado; `SharedFlow` es un stream de eventos sin valor inicial ni conflación por defecto.

**15. ¿Qué hace `stateIn(WhileSubscribed(5000))`?**
Convierte un Flow frío en StateFlow caliente, empieza con suscriptores y espera 5 s tras el último para cancelar. Evita reiniciar en rotaciones.

**16. ¿Qué pasa si falla una corrutina hija en `coroutineScope`?**
Se cancela el scope y se propaga la excepción; por eso el refresh devuelve `Result.failure`.

**17. ¿Por qué no atrapar `CancellationException` como error?**
Porque la cancelación es control de flujo, no un fallo. Se re-lanza.

**18. ¿En qué dispatcher corre `viewModelScope`?**
Main por defecto. Las llamadas suspend de Retrofit/Room se encargan de no bloquear Main.

**19. ¿Cómo testeás ViewModels?**
`StandardTestDispatcher`, `Dispatchers.setMain`, fakes de repositorio, `advanceUntilIdle` y Turbine.

### Datos

**20. ¿Por qué Room es la fuente de verdad?**
La UI observa la base; el refresh escribe en la base. Así la app funciona offline, no hay dos verdades y las escrituras locales se reflejan por igual.

**21. ¿Cómo evitás perder una transferencia al refrescar?**
En la demo, `refresh()` preserva los saldos locales conocidos y hace upsert del resto. En producción el backend sería autoritativo y usaría idempotencia.

**22. ¿Cómo garantizás atomicidad?**
`TransferDao.applyTransfer` es `@Transaction`: o se actualiza el saldo y se inserta el movimiento, o no se aplica nada.

**23. ¿Por qué guardás enums como String?**
Para que el esquema no dependa del ordinal y para poder agregar valores con fallback (`?: GIRO`).

**24. ¿Qué migraciones faltan?**
`fallbackToDestructiveMigration` no se usa; hay esquema exportado. En producción se agregan `Migration` versionadas y tests de migración.

**25. ¿Qué datos no guardarías?**
PIN, tokens en claro, PII innecesaria, CVV, datos completos de tarjeta. La demo no persiste nada de eso.

### Red

**26. ¿Cómo funciona el backend simulado?**
`DemoApiInterceptor` intercepta el request de OkHttp y devuelve JSON de assets con status 200/404. El resto del stack (Retrofit, converter, suspend) es real.

**27. ¿Cómo lo reemplazás por un backend?**
Quitás el interceptor de demo, cambiás `baseUrl`, configurás auth real y manejás errores/401.

**28. ¿Por qué DTOs separados del dominio?**
Para aislar el contrato del backend de la UI y permitir mapeos/validaciones explícitas.

**29. ¿Cómo manejás 401?**
Un `Authenticator` renueva el token y reintenta; si falla, se cierra sesión. En la demo el token es fijo.

**30. ¿Qué pasa si el POST se procesa pero falla la respuesta?**
En producción, con idempotency key, el reintento devuelve el mismo recibo. Sin eso, hay riesgo de duplicado.

### Banca/seguridad

**31. ¿Por qué centavos?**
Evita errores de punto flotante y permite aritmética exacta. Se formatea a EUR solo en presentación.

**32. ¿Diferencia balance/disponible?**
Balance es contable; disponible descuenta retenciones y límites. La transferencia valida disponible.

**33. ¿Cómo validás un IBAN?**
Normalización, longitud/forma, reordenamiento, letras a números, mod 97 == 1.

**34. ¿Qué es SCA?**
Strong Customer Authentication: autenticación reforzada para operaciones sensibles. La biometría de la demo no cumple sola; en producción se vincula a claves y al servidor.

**35. ¿Qué harías antes de producción?**
Token seguro, TLS pinning, Keystore, idempotencia, auditoría, límites/fraude, migraciones, encriptación local, tests instrumentados, signing y monitoreo.

### Testing/CI

**36. ¿Por qué fakes y no MockK?**
Los fakes son explícitos, legibles y no acoplan el test a detalles de mocking. MockK es válido; acá se eligió claridad.

**37. ¿Qué prueba cada suite?**
Dominio: reglas puras. ViewModels: transiciones y errores. UI: PIN correcto/incorrecto. CI: lint, tests, debug y release con R8.

**38. ¿Por qué R8 en CI?**
Para detectar temprano reglas faltantes de serialización/Retrofit y errores de resource shrinking.

**39. ¿Qué limitación tiene el lint?**
Se desactiva el análisis de test sources en `:app` por un crash de AGP/Kotlin 2.2; los tests de UI se ejecutan aparte. Es una limitación de tooling documentada.

**40. ¿Qué mejorarías del proyecto?**
Migraciones, tests instrumentados de flujo completo, paginación, localización, accesibilidad, encriptación, backend real con idempotencia y signing de release.

---

## 14. Plan de estudio para hoy y mañana

### Hoy (2–3 horas)

1. **30 min** — Corré la app y seguí el guion de demo. Anotá qué pantalla corresponde a qué archivo.
2. **45 min** — Leé `TransferViewModel`, `DefaultBankRepository` y `TransferDao`. Explicá en voz alta el camino de una transferencia.
3. **30 min** — Leé `AccountsViewModel` y `TransactionsViewModel`. Entendé `combine`, `stateIn` y los filtros.
4. **30 min** — Leé los tests. Elegí dos y explicá qué falla si se rompe el código.
5. **30 min** — Repasá Kotlin/Compose de las secciones 3 y 4. Escribí un ejemplo propio de `data class` + `copy`.

### Mañana (1–2 horas)

1. **20 min** — Repasá preguntas 1–15.
2. **20 min** — Repasá preguntas 20–35 (datos, red, banca).
3. **20 min** — Ensayá el guion de 6 minutos cronometrado.
4. **20 min** — Prepará 2–3 preguntas para el entrevistador:
   - ¿Cómo manejan idempotencia y reconciliación en transferencias?
   - ¿Qué grado de offline-first tiene la app real?
   - ¿Cómo separan SCA de la biometría del dispositivo?
5. **10 min** — Revisá que el emulador esté encendido y la app instalada con `./gradlew installDebug`.

---

## 15. Glosario

- **UDF (Unidirectional Data Flow)**: estado baja, eventos suben.
- **UiState**: objeto inmutable que describe todo lo que necesita la pantalla.
- **SSOT (Single Source of Truth)**: una sola fuente autoritativa; acá Room para la UI.
- **DTO**: objeto que representa el JSON de red.
- **Entity**: objeto que representa una tabla de Room.
- **Composable**: función que describe UI.
- **Recomposición**: reevaluación de composables cuando cambia el estado leído.
- **State hoisting**: mover el estado hacia arriba para que la pantalla sea reutilizable y testeable.
- **DI**: inyección de dependencias.
- **KSP**: procesador de símbolos de Kotlin; genera código de Room/Hilt/serialización.
- **Idempotencia**: repetir una operación no cambia el resultado.
- **SCA**: autenticación reforzada para operaciones sensibles.
- **R8**: shrinker/optimizador de código y recursos de Android.
- **Edge-to-edge**: dibujar detrás de las barras del sistema respetando insets.

---

## 16. Cierre

Si podés explicar estas tres cosas, la entrevista va bien:

1. **Por qué Room es la fuente de verdad** y cómo el flujo reactivo actualiza la UI.
2. **Cómo una transferencia se valida, se envía y se aplica atómicamente**, y qué falta para producción (idempotencia, backend autoritativo, SCA).
3. **Cómo está separada la UI del dominio**, con un `UiState` inmutable y tests que prueban las reglas sin Android.

El resto son detalles que podés buscar en el código. La ventaja de Kontor es que cada decisión tiene un archivo concreto que la respalda.
