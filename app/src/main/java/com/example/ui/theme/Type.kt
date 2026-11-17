package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// 100% Cyrillic-verified font families
val UnboundedFamily = FontFamily(Font(R.font.unbounded, FontWeight.Normal))
val RubikFamily = FontFamily(Font(R.font.rubik, FontWeight.Normal))
val ComfortaaFamily = FontFamily(Font(R.font.comfortaa, FontWeight.Normal))
val RobotoFlexFamily = FontFamily(Font(R.font.roboto_flex, FontWeight.Normal))

val LiterataFamily = FontFamily(Font(R.font.literata, FontWeight.Normal))
val LoraFamily = FontFamily(Font(R.font.lora, FontWeight.Normal))
val EbGaramondFamily = FontFamily(Font(R.font.eb_garamond, FontWeight.Normal))
val MerriweatherFamily = FontFamily(Font(R.font.merriweather, FontWeight.Normal))
val PlayfairDisplayFamily = FontFamily(Font(R.font.playfair_display, FontWeight.Normal))
val CormorantGaramondFamily = FontFamily(Font(R.font.cormorant_garamond, FontWeight.Normal))
val PtSerifFamily = FontFamily(Font(R.font.pt_serif, FontWeight.Normal))
val VollkornFamily = FontFamily(Font(R.font.vollkorn, FontWeight.Normal))

val BalsamiqSansFamily = FontFamily(Font(R.font.balsamiq_sans, FontWeight.Normal))
val ShantellSansFamily = FontFamily(Font(R.font.shantell_sans, FontWeight.Normal))
val PangolinFamily = FontFamily(Font(R.font.pangolin, FontWeight.Normal))
val NeuchaFamily = FontFamily(Font(R.font.neucha, FontWeight.Normal))

val CaveatFamily = FontFamily(Font(R.font.caveat, FontWeight.Normal))
val MarckScriptFamily = FontFamily(Font(R.font.marck_script, FontWeight.Normal))
val BadScriptFamily = FontFamily(Font(R.font.bad_script, FontWeight.Normal))
val AmaticScFamily = FontFamily(Font(R.font.amatic_sc, FontWeight.Normal))

val Tiny5Family = FontFamily(Font(R.font.tiny5, FontWeight.Normal))
val PressStart2PFamily = FontFamily(Font(R.font.press_start_2p, FontWeight.Normal))
val TekturFamily = FontFamily(Font(R.font.tektur, FontWeight.Normal))
val JuraFamily = FontFamily(Font(R.font.jura, FontWeight.Normal))
val KellySlabFamily = FontFamily(Font(R.font.kelly_slab, FontWeight.Normal))

val JetBrainsMonoFamily = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))

enum class ReaderFontOption(
  val title: String,
  val badge: String,
  val previewPhrase: String,
  val fontFamily: FontFamily,
) {
  LITERATA(
    title = "Literata",
    badge = "Книжный",
    previewPhrase = "Плавный ритм книжной строки",
    fontFamily = LiterataFamily,
  ),
  UNBOUNDED(
    title = "Unbounded",
    badge = "M3 Expressive",
    previewPhrase = "Выразительная геометрия M3",
    fontFamily = UnboundedFamily,
  ),
  BALSAMIQ_SANS(
    title = "Balsamiq Sans",
    badge = "Комикс",
    previewPhrase = "Тёплый дух Comic Sans MS",
    fontFamily = BalsamiqSansFamily,
  ),
  CAVEAT(
    title = "Caveat",
    badge = "Рукописный",
    previewPhrase = "Живой росчерк от руки",
    fontFamily = CaveatFamily,
  ),
  TINY5(
    title = "Tiny5 NDot",
    badge = "NDot",
    previewPhrase = "Точечная матрица 5×5",
    fontFamily = Tiny5Family,
  ),
  PLAYFAIR_DISPLAY(
    title = "Playfair",
    badge = "С засечками",
    previewPhrase = "Журнальная антиква с контрастом",
    fontFamily = PlayfairDisplayFamily,
  ),
  SHANTELL_SANS(
    title = "Shantell Sans",
    badge = "Комикс",
    previewPhrase = "Игривый маркерный шрифт",
    fontFamily = ShantellSansFamily,
  ),
  ROBOTO_FLEX(
    title = "Roboto Flex",
    badge = "M3 Expressive",
    previewPhrase = "Гибкий системный гротеск M3",
    fontFamily = RobotoFlexFamily,
  ),
  MARCK_SCRIPT(
    title = "Marck Script",
    badge = "Рукописный",
    previewPhrase = "Плавная перьевая каллиграфия",
    fontFamily = MarckScriptFamily,
  ),
  TEKTUR(
    title = "Tektur",
    badge = "NDot / Техно",
    previewPhrase = "Футуристичный модульный ритм",
    fontFamily = TekturFamily,
  ),
  PT_SERIF(
    title = "PT Serif",
    badge = "С засечками",
    previewPhrase = "Эталонная русская типографика",
    fontFamily = PtSerifFamily,
  ),
  PANGOLIN(
    title = "Pangolin",
    badge = "Комикс",
    previewPhrase = "Дружелюбный печатный почерк",
    fontFamily = PangolinFamily,
  ),
  RUBIK(
    title = "Rubik",
    badge = "M3 Expressive",
    previewPhrase = "Мягко скруглённые формы",
    fontFamily = RubikFamily,
  ),
  BAD_SCRIPT(
    title = "Bad Script",
    badge = "Рукописный",
    previewPhrase = "Свободная тетрадная запись",
    fontFamily = BadScriptFamily,
  ),
  CORMORANT_GARAMOND(
    title = "Cormorant",
    badge = "С засечками",
    previewPhrase = "Утончённая книжная классика",
    fontFamily = CormorantGaramondFamily,
  ),
  PRESS_START_2P(
    title = "Press Start",
    badge = "Пиксель",
    previewPhrase = "Ретро-аркада в каждой букве",
    fontFamily = PressStart2PFamily,
  ),
  NEUCHA(
    title = "Neucha",
    badge = "Комикс",
    previewPhrase = "Лёгкий скетч и мягкий тон",
    fontFamily = NeuchaFamily,
  ),
  COMFORTAA(
    title = "Comfortaa",
    badge = "Округлый",
    previewPhrase = "Воздушная округлая графика",
    fontFamily = ComfortaaFamily,
  ),
  VOLLKORN(
    title = "Vollkorn",
    badge = "С засечками",
    previewPhrase = "Плотная благородная антиква",
    fontFamily = VollkornFamily,
  ),
  AMATIC_SC(
    title = "Amatic SC",
    badge = "Рукописный",
    previewPhrase = "Высокие штрихи от руки",
    fontFamily = AmaticScFamily,
  ),
  JURA(
    title = "Jura",
    badge = "Техно",
    previewPhrase = "Чёткий техно-гротеск",
    fontFamily = JuraFamily,
  ),
  KELLY_SLAB(
    title = "Kelly Slab",
    badge = "Брусковый",
    previewPhrase = "Рубленые засечки и характер",
    fontFamily = KellySlabFamily,
  ),
  LORA(
    title = "Lora",
    badge = "Классика",
    previewPhrase = "Тёплые каллиграфические штрихи",
    fontFamily = LoraFamily,
  ),
  EB_GARAMOND(
    title = "EB Garamond",
    badge = "Классика",
    previewPhrase = "Эстетика классического тома",
    fontFamily = EbGaramondFamily,
  ),
  MERRIWEATHER(
    title = "Merriweather",
    badge = "Книжный",
    previewPhrase = "Высокая читаемость на экране",
    fontFamily = MerriweatherFamily,
  ),
  JETBRAINS_MONO(
    title = "JetBrains Mono",
    badge = "Моно",
    previewPhrase = "Чёткий моноширинный набор",
    fontFamily = JetBrainsMonoFamily,
  ),
}

val ExpressiveTypography =
  Typography(
    displayLarge =
      TextStyle(
        fontFamily = UnboundedFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.8).sp,
      ),
    displayMedium =
      TextStyle(
        fontFamily = UnboundedFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
      ),
    displaySmall =
      TextStyle(
        fontFamily = UnboundedFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.25).sp,
      ),
    headlineLarge =
      TextStyle(
        fontFamily = UnboundedFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 25.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.2).sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = RubikFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 29.sp,
      ),
    headlineSmall =
      TextStyle(
        fontFamily = PlayfairDisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = RubikFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = RubikFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
      ),
    titleSmall =
      TextStyle(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
      ),
    bodySmall =
      TextStyle(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = RubikFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
      ),
    labelMedium =
      TextStyle(
        fontFamily = RubikFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
      ),
    labelSmall =
      TextStyle(
        fontFamily = RobotoFlexFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp,
      ),
  )
