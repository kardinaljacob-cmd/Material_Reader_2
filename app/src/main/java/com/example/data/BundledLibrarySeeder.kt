package com.example.data

import android.content.Context
import com.example.data.db.BookDao
import com.example.data.model.BookEntity
import com.example.data.model.BookFormat
import com.example.data.model.BookmarkEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BundledLibrarySeeder {

  suspend fun seedIfEmpty(context: Context, bookDao: BookDao) =
    withContext(Dispatchers.IO) {
      val booksDir = File(context.filesDir, "books").apply { mkdirs() }
      val cosmosFile = File(booksDir, "cosmos_atlas.epub")
      val lighthouseFile = File(booksDir, "lighthouse_fjord.fb2")
      val gardenFile = File(booksDir, "geometric_garden.md")
      val richMarker = File(booksDir, ".rich_samples_v2")

      if (bookDao.getBookCount() > 0) {
        if (!richMarker.exists()) {
          if (cosmosFile.exists()) writeSampleEpub(cosmosFile)
          if (lighthouseFile.exists()) writeSampleFb2(lighthouseFile)
          if (gardenFile.exists()) writeSampleMarkdown(gardenFile)
          richMarker.writeText("ok")
        }
        return@withContext
      }

      val now = System.currentTimeMillis()

      writeSampleEpub(cosmosFile)
      writeSampleFb2(lighthouseFile)
      writeSampleMarkdown(gardenFile)

      val guideFile = File(booksDir, "listay_guide.txt")
      guideFile.writeText(
        """
        Глава 1. Добро пожаловать в Material Reader

        Material Reader создана для тех, кто любит не только читать, но и чувствовать красоту интерфейса. Здесь крупные выразительные формы Material 3 Expressive сочетаются с адаптивной палитрой ваших обоев и мягким тактильным откликом.

        Нажмите на центр экрана во время чтения, чтобы открыть панель инструментов. Вы можете переключаться между тремя плавными режимами чтения: объёмным 3D-листанием, глубинным параллаксом или вертикальным свитком с живым прогресс-баром и всплывающими сносками.

        Глава 2. Шрифты и типографика

        В читалке встроены 26 тщательно отобранных кириллических гарнитур: выразительные шрифты Material 3 Expressive (Unbounded, Rubik, Comfortaa, Roboto Flex), комикс-гарнитуры в духе Comic Sans MS (Balsamiq Sans, Shantell Sans, Pangolin, Neucha), рукописные, точечный NDot (Tiny5) и книжная классика с засечками.

        Попробуйте настроить размер кегля, межстрочный, межбуквенный, межсловесный и межобзацевый интервалы в «Расширенных настройках» — всё обновляется мгновенно в реальном времени.

        Глава 3. Ваши форматы и библиотека

        Нажмите кнопку «Добавить» на главном экране, чтобы добавить собственные файлы в форматах EPUB, FB2, FB2.ZIP, PDF, TXT, HTML или Markdown прямо с устройства. Все книги, закладки и прогресс чтения бережно сохраняются на вашем устройстве.
        """
          .trimIndent(),
        Charsets.UTF_8,
      )
      richMarker.writeText("ok")

      val cosmosId =
        bookDao.insertBook(
          BookEntity(
            title = "Звёздный атлас тишины",
            author = "Александр Соколов",
            formatName = BookFormat.EPUB.name,
            filePath = cosmosFile.absolutePath,
            bundledCoverKey = "cosmos",
            totalChapters = 4,
            currentChapterIndex = 1,
            currentPageInChapter = 0,
            progressPercent = 0.34f,
            lastReadTimestamp = now - 60_000L,
            addedTimestamp = now - 300_000L,
            isFavorite = true,
            accentColorHex = 0xFF3F2E8D,
            fileSizeBytes = cosmosFile.length(),
            description =
              "Очерки о природе времени, далёких туманностях и тихом свете, который идёт к нам сквозь миллиарды лет.",
          )
        )

      bookDao.insertBook(
        BookEntity(
          title = "Маяк на краю фьорда",
          author = "Элина Вересова",
          formatName = BookFormat.FB2.name,
          filePath = lighthouseFile.absolutePath,
          bundledCoverKey = "lighthouse",
          totalChapters = 4,
          currentChapterIndex = 0,
          currentPageInChapter = 0,
          progressPercent = 0.12f,
          lastReadTimestamp = now - 3_600_000L,
          addedTimestamp = now - 600_000L,
          isFavorite = true,
          accentColorHex = 0xFF0E7C5B,
          fileSizeBytes = lighthouseFile.length(),
          description =
            "Северная повесть о старом смотрителе маяка, лавандовых волнах и письмах, найденных в медной шкатулке.",
        )
      )

      bookDao.insertBook(
        BookEntity(
          title = "Сад геометрических снов",
          author = "Кирилл Артемьев",
          formatName = BookFormat.MD.name,
          filePath = gardenFile.absolutePath,
          bundledCoverKey = "garden",
          totalChapters = 3,
          currentChapterIndex = 0,
          currentPageInChapter = 0,
          progressPercent = 0.0f,
          lastReadTimestamp = now - 7_200_000L,
          addedTimestamp = now - 900_000L,
          isFavorite = false,
          accentColorHex = 0xFFD97706,
          fileSizeBytes = gardenFile.length(),
          description = "Поэтические эссе о ритме, тишине и архитектуре книжной страницы.",
        )
      )

      bookDao.insertBook(
        BookEntity(
          title = "Манифест экспрессивной формы",
          author = "Студия Material Reader",
          formatName = BookFormat.TXT.name,
          filePath = guideFile.absolutePath,
          bundledCoverKey = null,
          totalChapters = 3,
          currentChapterIndex = 0,
          currentPageInChapter = 0,
          progressPercent = 0.0f,
          lastReadTimestamp = now - 14_400_000L,
          addedTimestamp = now - 1_200_000L,
          isFavorite = false,
          accentColorHex = 0xFFE64833,
          fileSizeBytes = guideFile.length(),
          description = "Путеводитель по возможностям читалки, шрифтам и жестам.",
        )
      )

      bookDao.insertBookmark(
        BookmarkEntity(
          bookId = cosmosId,
          chapterIndex = 0,
          chapterTitle = "Глава I. Архитектура света",
          pageIndex = 0,
          excerpt =
            "Тишина космоса — не пустота, а самая вместительная библиотека во Вселенной...",
          colorIndex = 0,
        )
      )
    }

  private fun writeSampleMarkdown(file: File) {
    file.writeText(
      """
      # Сад геометрических снов

      Эссе о ритме, пространстве и красоте **живой строки**[^1].

      ## I. Линия и дыхание

      Хорошая страница дышит. Поля вокруг текста — это не пустое место, а *тихая рама*, которая позволяет взгляду отдыхать на каждом повороте мысли.

      > Форма становится выразительной тогда, когда в ней чувствуется движение человеческой руки и живой упругий отклик.

      Когда мы меняем гарнитуру шрифта, меняется сама интонация текста. Классическая антиква звучит как тёплый вечерний разговор у камина, а современный гибкий гротеск — как утренняя прогулка по свежему городу[^2].

      ## II. Архитектура утреннего сада

      Солнце поднимается над зелёными арками террасы. Листья отбрасывают мягкие тени на тёплый камень, складываясь в узор, похожий на иллюстрацию из старого альбома.

      В тишине утра легко услышать, как время складывается из мелочей: глотка горячего чая, шелеста перевёрнутого листа, лёгкой вибрации отклика под подушечкой пальца.

      ## III. Код тишины

      ```
      ритм = межстрочие * воздух
      уют = свет + любимый_шрифт
      чтение(книга, страница)
      ```

      Пусть каждая прочитанная глава оставляет после себя лёгкое послевкусие и желание открыть следующую историю.

      [^1]: В классической швейцарской типографике соотношение полей и кегля называют «музыкальной сеткой страницы».
      [^2]: Попробуйте сменить шрифт на Playfair Display или Comfortaa — характер эссе преобразится мгновенно.
      """
        .trimIndent(),
      Charsets.UTF_8,
    )
  }

  private fun writeSampleEpub(file: File) {
    val title = "Звёздный атлас тишины"
    val author = "Александр Соколов"
    val description =
      "Очерки о природе времени, далёких туманностях и тихом свете, который идёт к нам сквозь миллиарды лет."

    val css =
      """
      h1 { text-align: center; margin-bottom: 12pt; }
      .epigraph { text-align: right; font-style: italic; text-indent: 0; margin-bottom: 10pt; }
      .dropnote { text-indent: 0; font-variant: small-caps; }
      .center-sig { text-align: center; text-indent: 0; font-style: italic; margin-top: 12pt; }
      .noteref { vertical-align: super; font-size: 0.78em; font-weight: bold; }
      """.trimIndent()

    val chapterHtmls =
      listOf(
        "Глава I. Архитектура света" to
          """
          <h1>Глава I. Архитектура света</h1>
          <p class="epigraph">Свет старых звёзд дольше всего помнит путь домой.</p>
          <p class="dropnote">Когда мы поднимаем взгляд к ночному небу вдали от городских огней, мы смотрим не в пространство — мы смотрим вглубь времени<a epub:type="noteref" class="noteref" href="#fn1">[1]</a>. Каждый <em>фотон</em>, касающийся сетчатки, начал свой путь задолго до появления первых горных хребтов на Земле.</p>
          <blockquote>Тишина космоса — не пустота, а самая вместительная библиотека во Вселенной, где каждая орбита записана гравитацией.</blockquote>
          <p>В этой книге собраны путевые заметки о том, как устроены невидимые мосты между атомами звёздной пыли и мыслящим человеком, который учится читать <strong>язык созвездий</strong>.</p>
          <p>Посмотрите на созвездие Ориона: две соседние точки света разделены сотнями световых лет<a epub:type="noteref" class="noteref" href="#fn2">[2]</a>, хотя на бархатном полотне неба они кажутся неразлучными сёстрами. Наше воображение соединяет их в рисунок, превращая хаос расстояний в понятный узор.</p>
          <aside epub:type="footnote" id="fn1"><p>[1] Свет от туманности Андромеды путешествует до Земли около 2,5 миллионов лет.</p></aside>
          <aside epub:type="footnote" id="fn2"><p>[2] Звезда Ригель светит примерно в 120 000 раз ярче Солнца и находится в 860 световых годах от нас.</p></aside>
          """.trimIndent(),
        "Глава II. Музыка орбитальных колец" to
          """
          <h1>Глава II. Музыка орбитальных колец</h1>
          <p>Вокруг планет-гигантов вращаются миллиарды ледяных осколков. Издалека они сливаются в идеальный сияющий диск, словно золотая пластинка, поставленная на невидимый проигрыватель.</p>
          <p>Гравитационные <em>резонансы</em><a epub:type="noteref" class="noteref" href="#fn3">[3]</a> вырезают в этих кольцах тончайшие щели, создавая математическую симметрию там, где, казалось бы, царит лишь холодный камень и лёд.</p>
          <p>Когда космический аппарат проходит сквозь тень таких колец, солнечный свет преломляется в пылинках, окрашивая горизонт в оттенки тёплой охры и глубокого индиго.</p>
          <aside epub:type="footnote" id="fn3"><p>[3] Щель Кассини шириной 4 800 км возникла из-за орбитального резонанса 2:1 со спутником Мимас.</p></aside>
          """.trimIndent(),
        "Глава III. Память красных пустынь" to
          """
          <h1>Глава III. Память красных пустынь</h1>
          <p>Оксид железа подарил четвёртой планете её неповторимый терракотовый цвет. В руслах высохших рек до сих пор хранятся слои древней глины — немые свидетели эпохи, когда над марсианскими каньонами<a epub:type="noteref" class="noteref" href="#fn4">[4]</a> плыли тяжёлые дождевые облака.</p>
          <p>Ветер там почти невесом, но за миллионы лет он вытачивает из базальтовых скал причудливые арки и дюны с острыми гребнями.</p>
          <p>Человек стремится туда не только ради науки, но и потому, что горизонт всегда зовёт дальше, чем достаёт взгляд.</p>
          <aside epub:type="footnote" id="fn4"><p>[4] Долина Маринер простирается более чем на 4 000 км и достигает глубины свыше 7 км.</p></aside>
          """.trimIndent(),
        "Глава IV. Часы на краю горизонта" to
          """
          <h1>Глава IV. Часы на краю горизонта</h1>
          <p>Близ массивных звёзд само течение секунд меняет ритм. То, что для далёкого наблюдателя длится столетие, вблизи горизонта событий укладывается в несколько вдохов<a epub:type="noteref" class="noteref" href="#fn5">[5]</a>.</p>
          <p>Время не течёт единой рекой — оно изгибается, замедляется и расходится рукавами, как свет в гравитационной линзе. Читать книгу о космосе — значит на пару часов освободиться от спешки земных будильников.</p>
          <p class="center-sig">* * * Конец первой части атласа * * *</p>
          <aside epub:type="footnote" id="fn5"><p>[5] Гравитационное замедление времени подтверждается даже на орбите Земли — спутники GPS учитывают поправку около 38 микросекунд в сутки.</p></aside>
          """.trimIndent(),
      )

    val baos = ByteArrayOutputStream()
    ZipOutputStream(baos).use { zos ->
      zos.putNextEntry(ZipEntry("mimetype"))
      zos.write("application/epub+zip".toByteArray(Charsets.UTF_8))
      zos.closeEntry()

      zos.putNextEntry(ZipEntry("META-INF/container.xml"))
      zos.write(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
          <rootfiles>
            <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
          </rootfiles>
        </container>
        """
          .trimIndent()
          .toByteArray(Charsets.UTF_8)
      )
      zos.closeEntry()

      zos.putNextEntry(ZipEntry("OEBPS/styles.css"))
      zos.write(css.toByteArray(Charsets.UTF_8))
      zos.closeEntry()

      val manifestItems =
        """<item id="css" href="styles.css" media-type="text/css"/>""" +
          "\n" +
          chapterHtmls.indices.joinToString("\n") { idx ->
            """<item id="ch$idx" href="chapter_$idx.xhtml" media-type="application/xhtml+xml"/>"""
          }
      val spineItems =
        chapterHtmls.indices.joinToString("\n") { idx -> """<itemref idref="ch$idx"/>""" }

      zos.putNextEntry(ZipEntry("OEBPS/content.opf"))
      zos.write(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <package xmlns="http://www.idpf.org/2007/opf" version="2.0">
          <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
            <dc:title>$title</dc:title>
            <dc:creator>$author</dc:creator>
            <dc:description>$description</dc:description>
          </metadata>
          <manifest>
            $manifestItems
          </manifest>
          <spine>
            $spineItems
          </spine>
        </package>
        """
          .trimIndent()
          .toByteArray(Charsets.UTF_8)
      )
      zos.closeEntry()

      chapterHtmls.forEachIndexed { idx, (chTitle, bodyHtml) ->
        zos.putNextEntry(ZipEntry("OEBPS/chapter_$idx.xhtml"))
        zos.write(
          """
          <?xml version="1.0" encoding="UTF-8"?>
          <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
          <head>
            <title>$chTitle</title>
            <link rel="stylesheet" type="text/css" href="styles.css"/>
          </head>
          <body>
            $bodyHtml
          </body>
          </html>
          """
            .trimIndent()
            .toByteArray(Charsets.UTF_8)
        )
        zos.closeEntry()
      }
    }
    file.writeBytes(baos.toByteArray())
  }

  private fun writeSampleFb2(file: File) {
    val fb2 =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <FictionBook xmlns="http://www.gribuser.ru/xml/fictionbook/2.0" xmlns:l="http://www.w3.org/1999/xlink">
        <description>
          <title-info>
            <genre>prose_contemporary</genre>
            <author>
              <first-name>Элина</first-name>
              <last-name>Вересова</last-name>
            </author>
            <book-title>Маяк на краю фьорда</book-title>
            <annotation>
              <p>Северная повесть о старом смотрителе маяка, лавандовых волнах и письмах, найденных в медной шкатулке.</p>
            </annotation>
          </title-info>
        </description>
        <body>
          <section>
            <title><p>Глава 1. Коралловая башня</p></title>
            <epigraph><p>Море помнит каждое судно, которому помогли вернуться домой.</p></epigraph>
            <p>На самом краю изумрудного утёса, где ветер пахнет солью и можжевельником, стоял невысокий <strong>коралловый маяк</strong>. Его стены давно выцвели под северным солнцем, приобретя оттенок спелой хурмы.</p>
            <p>Каждое утро Хенрик поднимался по ста двадцати чугунным ступеням, чтобы протереть гранёные линзы <emphasis>Френеля</emphasis><a l:href="#n1" type="note">[1]</a> и записать в толстый судовой журнал направление прилива.</p>
            <p>В тот ноябрьский вторник море было необычного фиолетового оттенка — словно небо уронило в воду горсть сумерек.</p>
          </section>
          <section>
            <title><p>Глава 2. Медная шкатулка</p></title>
            <p>В нише под винтовой лестницей, за стопкой старых навигационных карт<a l:href="#n2" type="note">[2]</a>, Хенрик обнаружил тяжёлую медную шкатулку с выгравированной розой ветров.</p>
            <p>Внутри лежали семь писем, перевязанных бечёвкой, и маленькая стеклянная призма. На верхнем конверте чернила почти поблекли, но всё ещё можно было разобрать дату: <emphasis>октябрь 1968 года</emphasis>.</p>
            <cite><p>«Тому, кто зажжёт огонь в туманную ночь: не торопись закрывать окно на восток — там приходят самые честные ветры.»</p></cite>
          </section>
          <section>
            <title><p>Глава 3. Песня стеклянной призмы</p></title>
            <p>Когда луч маяка прошёл сквозь найденную призму, стены башни вспыхнули семью широкими полосами цвета — от янтарного до глубокого ультрамарина.</p>
            <p>Вечером к причалу впервые за месяц подошёл почтовый катер. Капитан помахал рукой и крикнул сквозь шум прибоя, что сегодня свет маяка был виден за двадцать миль — ярче обычного.</p>
          </section>
          <section>
            <title><p>Глава 4. Свет в окне на восток</p></title>
            <p>Хенрик заварил крепкий чай с чабрецом и открыл чистую страницу журнала. За окном шумели волны, но внутри башни было тепло, спокойно и светло.</p>
            <p>Он понял: маяки нужны не только кораблям в океане. Они нужны людям на берегу, чтобы знать, что свет никогда не гаснет напрасно.</p>
          </section>
        </body>
        <body name="notes">
          <section id="n1">
            <title><p>[1]</p></title>
            <p>Ступенчатая линза, изобретённая физиком Огюстеном Френелем в 1822 году: собирает свет лампы в узкий яркий луч, заметный за десятки миль.</p>
          </section>
          <section id="n2">
            <title><p>[2]</p></title>
            <p>Старинные норвежские лоции с ручной штриховкой глубин и пометками о приливных течениях у берегов Лофотена.</p>
          </section>
        </body>
      </FictionBook>
      """
        .trimIndent()
    file.writeText(fb2, Charsets.UTF_8)
  }
}
