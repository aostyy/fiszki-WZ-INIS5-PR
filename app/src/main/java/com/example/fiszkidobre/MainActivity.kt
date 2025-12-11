package com.example.fiszkidobre

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ==================== ENCJA ====================

@Entity(tableName = "fiszki")
data class FiszkaEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "fiszkiID") val fiszkiID: Int = 0,
    @ColumnInfo(name = "lekcjaID") val lekcjaID: Int? = null,
    @ColumnInfo(name = "pytanie") val pytanie: String,
    @ColumnInfo(name = "odpowiedz") val odpowiedz: String,
    @ColumnInfo(name = "zla_odpowiedz1") val zla_odpowiedz1: String? = null,
    @ColumnInfo(name = "zla_odpowiedz2") val zla_odpowiedz2: String? = null,
    @ColumnInfo(name = "zla_odpowiedz3") val zla_odpowiedz3: String? = null
)

// ==================== DAO ====================

@Dao
interface FiszkiDao {
    @Query("SELECT * FROM fiszki WHERE lekcjaID = :lessonId")
    suspend fun getFiszkiByLesson(lessonId: Int): List<FiszkaEntity>

    @Insert
    suspend fun insertFiszka(fiszka: FiszkaEntity)

    @Insert
    suspend fun insertAll(fiszki: List<FiszkaEntity>)

    @Query("DELETE FROM fiszki")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM fiszki")
    suspend fun count(): Int
}

// ==================== BAZA ====================

@Database(entities = [FiszkaEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fiszkiDao(): FiszkiDao

    companion object {
        // Singleton dla bazy danych
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fiszki_app.db"  // Nazwa bazy w systemie plików
                )
                    .fallbackToDestructiveMigration() // Jeśli schemat się zmieni, usuwa starą bazę
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==================== VIEWMODEL ====================

class LessonViewModel(private val dao: FiszkiDao) : ViewModel() {

    var fiszkiList by mutableStateOf<List<FiszkaEntity>>(emptyList())
        private set

    var currentIndex by mutableStateOf(0)
        private set

    var isLoading by mutableStateOf(false)
        private set

    val currentFiszka: FiszkaEntity?
        get() = fiszkiList.getOrNull(currentIndex)

    init {
        // Przy inicjalizacji sprawdź czy są dane, jeśli nie - dodaj przykładowe
        checkAndPopulateDatabase()
    }

    private fun checkAndPopulateDatabase() {
        viewModelScope.launch {
            isLoading = true
            try {
                val count = dao.count()
                Log.d("LessonViewModel", "Liczba fiszek w bazie: $count")

                if (count == 0) {
                    Log.d("LessonViewModel", "Baza jest pusta, dodaję przykładowe dane")
                    insertSampleData()
                } else {
                    Log.d("LessonViewModel", "Baza już zawiera dane")
                }
            } catch (e: Exception) {
                Log.e("LessonViewModel", "Błąd przy sprawdzaniu bazy: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun insertSampleData() {
        val sampleFiszki = listOf(
            FiszkaEntity(
                lekcjaID = 1,
                pytanie = "Co oznacza słowo 'kot' po angielsku?",
                odpowiedz = "Cat",
                zla_odpowiedz1 = "Dog",
                zla_odpowiedz2 = "Mouse",
                zla_odpowiedz3 = "Bird"
            ),
            FiszkaEntity(
                lekcjaID = 1,
                pytanie = "Co oznacza słowo 'pies' po angielsku?",
                odpowiedz = "Dog",
                zla_odpowiedz1 = "Cat",
                zla_odpowiedz2 = "Fish",
                zla_odpowiedz3 = "Rabbit"
            ),
            FiszkaEntity(
                lekcjaID = 1,
                pytanie = "Co oznacza słowo 'dom' po angielsku?",
                odpowiedz = "House",
                zla_odpowiedz1 = "Car",
                zla_odpowiedz2 = "Tree",
                zla_odpowiedz3 = "Garden"
            ),
            FiszkaEntity(
                lekcjaID = 1,
                pytanie = "Co oznacza słowo 'książka' po angielsku?",
                odpowiedz = "Book",
                zla_odpowiedz1 = "Pen",
                zla_odpowiedz2 = "Paper",
                zla_odpowiedz3 = "Table"
            ),
            FiszkaEntity(
                lekcjaID = 1,
                pytanie = "Co oznacza słowo 'samochód' po angielsku?",
                odpowiedz = "Car",
                zla_odpowiedz1 = "Bike",
                zla_odpowiedz2 = "Bus",
                zla_odpowiedz3 = "Train"
            ),
            FiszkaEntity(
                lekcjaID = 2,
                pytanie = "Jak powiesz 'cześć' po angielsku?",
                odpowiedz = "Hello",
                zla_odpowiedz1 = "Goodbye",
                zla_odpowiedz2 = "Please",
                zla_odpowiedz3 = "Thank you"
            ),
            FiszkaEntity(
                lekcjaID = 2,
                pytanie = "Jak powiesz 'dziękuję' po angielsku?",
                odpowiedz = "Thank you",
                zla_odpowiedz1 = "Sorry",
                zla_odpowiedz2 = "Please",
                zla_odpowiedz3 = "Excuse me"
            ),
            FiszkaEntity(
                lekcjaID = 2,
                pytanie = "Jak powiesz 'przepraszam' po angielsku?",
                odpowiedz = "Sorry",
                zla_odpowiedz1 = "Thank you",
                zla_odpowiedz2 = "Hello",
                zla_odpowiedz3 = "Goodbye"
            )
        )

        dao.insertAll(sampleFiszki)
        Log.d("LessonViewModel", "Dodano ${sampleFiszki.size} przykładowych fiszek")
    }

    fun loadLesson(lessonId: Int) {
        viewModelScope.launch {
            isLoading = true
            try {
                Log.d("LessonViewModel", "Ładowanie lekcji ID: $lessonId")
                fiszkiList = dao.getFiszkiByLesson(lessonId)
                Log.d("LessonViewModel", "Załadowano ${fiszkiList.size} fiszek")
                currentIndex = 0

                if (fiszkiList.isEmpty()) {
                    Log.w("LessonViewModel", "Brak fiszek dla lekcji $lessonId!")
                }
            } catch (e: Exception) {
                Log.e("LessonViewModel", "Błąd ładowania lekcji: ${e.message}")
                fiszkiList = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    fun next() {
        if (currentIndex < fiszkiList.lastIndex) {
            currentIndex += 1
            Log.d("LessonViewModel", "Przechodzę do fiszki $currentIndex")
        } else {
            Log.d("LessonViewModel", "To już ostatnia fiszka!")
            // Możesz dodać komunikat że to koniec lekcji
        }
    }

    fun resetLesson() {
        currentIndex = 0
    }
}

// ==================== MAIN ACTIVITY ====================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "Aplikacja startuje")

        // Pobierz instancję bazy danych
        val database = AppDatabase.getDatabase(applicationContext)
        val viewModel = LessonViewModel(database.fiszkiDao())

        setContent {
            FiszkiApp(viewModel)
        }
    }
}

// ==================== GŁÓWNY EKRAN APLIKACJI ====================

@Composable
fun FiszkiApp(viewModel: LessonViewModel) {
    var selectedLessonId by remember { mutableStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Wybór lekcji
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    selectedLessonId = 1
                    viewModel.loadLesson(1)
                    viewModel.resetLesson()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedLessonId == 1) Color.Green else MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Lekcja 1: Zwierzęta")
            }

            Button(
                onClick = {
                    selectedLessonId = 2
                    viewModel.loadLesson(2)
                    viewModel.resetLesson()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedLessonId == 2) Color.Green else MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Lekcja 2: Powitania")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ekran z pytaniem
        FiszkaQuestionScreen(viewModel)
    }
}

// ==================== EKRAN Z PYTANIEM ====================

@Composable
fun FiszkaQuestionScreen(viewModel: LessonViewModel) {
    val fiszka = viewModel.currentFiszka
    val isLoading by remember { derivedStateOf { viewModel.isLoading } }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (fiszka == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Brak fiszek w tej lekcji", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.loadLesson(1) }) {
                    Text("Załaduj ponownie")
                }
            }
        }
        return
    }

    val answers = remember(fiszka.fiszkiID) {
        listOf<Pair<String, Boolean>>(
            Pair(fiszka.odpowiedz, true),
            Pair(fiszka.zla_odpowiedz1 ?: "Brak odpowiedzi", false),
            Pair(fiszka.zla_odpowiedz2 ?: "Brak odpowiedzi", false),
            Pair(fiszka.zla_odpowiedz3 ?: "Brak odpowiedzi", false)
        ).shuffled()
    }

    var selectedAnswerIndex by remember { mutableStateOf<Int?>(null) }
    var showResult by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Numer fiszki
        Text(
            text = "Fiszka ${viewModel.currentIndex + 1} z ${viewModel.fiszkiList.size}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(Modifier.height(8.dp))

        // Pytanie
        Text(
            text = fiszka.pytanie,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // Odpowiedzi
        answers.forEachIndexed { index, pair ->
            val (text, isCorrect) = pair

            Button(
                onClick = {
                    if (selectedAnswerIndex == null) {
                        selectedAnswerIndex = index
                        showResult = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        selectedAnswerIndex == null -> MaterialTheme.colorScheme.primary
                        index == selectedAnswerIndex && isCorrect -> Color.Green
                        index == selectedAnswerIndex && !isCorrect -> Color.Red
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                ),
                enabled = selectedAnswerIndex == null
            ) {
                Text(text = text, modifier = Modifier.padding(8.dp))
            }
        }

        Spacer(Modifier.height(32.dp))

        // Przycisk dalej
        if (showResult) {
            Button(
                onClick = {
                    if (selectedAnswerIndex != null &&
                        answers[selectedAnswerIndex!!].second) {
                        viewModel.next()
                    }
                    selectedAnswerIndex = null
                    showResult = false
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedAnswerIndex != null &&
                        answers[selectedAnswerIndex!!].second) Color.Green else Color.Red
                )
            ) {
                Text(if (selectedAnswerIndex != null &&
                    answers[selectedAnswerIndex!!].second) "Dobrze! Następna fiszka"
                else "Źle! Spróbuj dalej")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Statystyki
        Text(
            text = "Pozostało fiszek: ${viewModel.fiszkiList.size - viewModel.currentIndex - 1}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

// ==================== PREVIEW ====================

@Preview(showBackground = true)
@Composable
fun Preview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Podgląd aplikacji fiszek")
        }
    }
}