package ru.my.mazda

/**
 * Регламент ТО: Mazda Axela (BM) 2018, 1.5 SkyActiv-G (P5-VPS), автомат, передний привод.
 *
 * Интервалы — по сервисной книжке Mazda (Европа): ТО каждые 20 000 км / 12 мес,
 * в тяжёлых условиях чаще. Для масла взят интервал дилеров в России (15 000 км).
 * «Тяжёлые условия» — это морозы, короткие поездки, пробки, пыль: для Сибири это норма.
 *
 * Чтобы поменять интервал или артикул — правь строки ниже, приложение пересоберётся само.
 */

data class PartNo(val what: String, val number: String)

data class Service(
    val id: String,
    val title: String,
    /** Иллюстрации узла: первая — главная. */
    val pics: List<Int>,
    /** «Замена», «Проверка»… */
    val action: String,
    val km: Int?,
    val months: Int?,
    val kmHard: Int?,
    val monthsHard: Int?,
    /** Первая замена считается от нового авто (антифриз). */
    val firstKm: Int? = null,
    val firstMonths: Int? = null,
    val what: String,
    val fluid: String? = null,
    val volume: String? = null,
    val parts: List<PartNo> = emptyList(),
    val tips: String = "",
    /** true — по регламенту Mazda, false — рекомендация сверх регламента. */
    val official: Boolean = true,
)

object Regulation {

    val all: List<Service> = listOf(
        Service(
            id = "oil",
            title = "Масло и масляный фильтр",
            pics = listOf(R.drawable.pic_oil, R.drawable.pic_oil_filter),
            action = "Замена",
            km = 15_000, months = 12,
            kmHard = 7_500, monthsHard = 6,
            what = "Слить моторное масло, заменить масляный фильтр и шайбу сливной пробки, залить новое масло до верхней метки щупа.",
            fluid = "0W-20 (Mazda Original Oil Supra 0W-20) или 5W-30. Допуск API SN / ILSAC GF-5.",
            volume = "≈ 4 л с фильтром. Заливай 3,5 л, дальше доливай по щупу.",
            parts = listOf(
                PartNo("Масляный фильтр", "PE01-14-302A"),
                PartNo("Шайба сливной пробки", "9956-41-400"),
            ),
            tips = "В книжке Mazda для Европы — 20 000 км, но при морозах, пробках и коротких поездках масло стареет быстрее: в тяжёлых условиях Mazda сама просит менять каждые 10 000 км и чаще. После замены сбрось индикатор ключа на приборке.",
        ),
        Service(
            id = "air_filter",
            title = "Воздушный фильтр двигателя",
            pics = listOf(R.drawable.pic_air_filter),
            action = "Замена",
            km = 60_000, months = 36,
            kmHard = 30_000, monthsHard = 24,
            what = "Открыть корпус воздушного фильтра (защёлки), поменять фильтрующий элемент.",
            parts = listOf(PartNo("Воздушный фильтр", "PE07-13-3A0A")),
            tips = "Летом по грунтовкам и в пыль — проверяй чаще: забитый фильтр съедает мощность и повышает расход.",
        ),
        Service(
            id = "cabin_filter",
            title = "Салонный фильтр",
            pics = listOf(R.drawable.pic_cabin_filter),
            action = "Замена",
            km = 40_000, months = 24,
            kmHard = 20_000, monthsHard = 12,
            what = "Снять бардачок (отщёлкнуть ограничители), достать старый фильтр, поставить новый стрелкой по потоку воздуха.",
            parts = listOf(PartNo("Салонный фильтр", "KD45-61-J6X")),
            tips = "Угольный фильтр лучше держит запахи. Если запотевают стёкла и слабо дует печка — первым делом сюда.",
        ),
        Service(
            id = "spark_plugs",
            title = "Свечи зажигания",
            pics = listOf(R.drawable.pic_spark_plug),
            action = "Замена",
            km = 120_000, months = null,
            kmHard = 120_000, monthsHard = null,
            what = "Снять катушки, выкрутить свечи на холодном двигателе, вкрутить новые с моментом затяжки по мануалу.",
            parts = listOf(
                PartNo("Свеча зажигания, 4 шт", "PE5R-18-110"),
                PartNo("Аналог NGK", "ILKAR7L11"),
            ),
            tips = "Свечи иридиевые, ресурс большой. Признаки, что пора раньше: троение, тяжёлый пуск в мороз, рост расхода.",
        ),
        Service(
            id = "brake_fluid",
            title = "Тормозная жидкость",
            pics = listOf(R.drawable.pic_brake_fluid),
            action = "Замена",
            km = 40_000, months = 24,
            kmHard = 40_000, monthsHard = 12,
            what = "Полная замена с прокачкой всех тормозных механизмов.",
            fluid = "DOT-4 (по мануалу допускается DOT-3).",
            volume = "≈ 1 л на замену с прокачкой.",
            tips = "Жидкость впитывает влагу из воздуха и со временем хуже работает — поэтому срок по годам, а не только по пробегу.",
        ),
        Service(
            id = "coolant",
            title = "Антифриз",
            pics = listOf(R.drawable.pic_coolant),
            action = "Замена",
            km = 100_000, months = 60,
            kmHard = 100_000, monthsHard = 60,
            firstKm = 200_000, firstMonths = 120,
            what = "Слить охлаждающую жидкость, промыть при необходимости, залить новую, удалить воздух из системы.",
            fluid = "Mazda FL-22 (зелёный). Другие антифризы Mazda не рекомендует.",
            volume = "≈ 6 л (весь объём системы).",
            tips = "Первая замена — через 200 000 км или 10 лет от нового авто, дальше каждые 100 000 км или 5 лет. Если не знаешь, меняли ли антифриз, отметь последнюю замену — иначе срок считается от даты выпуска машины.",
        ),
        Service(
            id = "atf",
            title = "Масло в АКПП",
            pics = listOf(R.drawable.pic_atf),
            action = "Частичная замена",
            km = 40_000, months = 48,
            kmHard = 30_000, monthsHard = 36,
            what = "Слить масло через сливную пробку коробки и залить столько же нового, проверить уровень на прогретой коробке.",
            fluid = "Mazda ATF FZ — только его.",
            volume = "≈ 3,5 л за одну частичную замену (всего в коробке ≈ 7,8 л).",
            tips = "По книжке Mazda масло в автомате не меняется, но при наших зимах и пробках мастера советуют частичную замену — это рекомендация, не заводской регламент.",
            official = false,
        ),
        Service(
            id = "belt",
            title = "Приводной ремень",
            pics = listOf(R.drawable.pic_belt),
            action = "Проверка",
            km = 60_000, months = 36,
            kmHard = 20_000, monthsHard = 12,
            what = "Осмотреть ремень навесного оборудования: трещины, расслоение, свист на холодную.",
            parts = listOf(PartNo("Приводной ремень", "подбирать по VIN")),
            tips = "Обычно меняют по состоянию, примерно в 80–120 тыс. км. ГРМ здесь на цепи — её по регламенту не меняют.",
        ),
        Service(
            id = "brakes",
            title = "Тормозные колодки и диски",
            pics = listOf(R.drawable.pic_brakes),
            action = "Проверка",
            km = 20_000, months = 12,
            kmHard = 20_000, monthsHard = 12,
            what = "Проверить толщину колодок и дисков, направляющие суппортов, тормозные шланги.",
            parts = listOf(PartNo("Колодки передние / задние", "подбирать по VIN")),
            tips = "Скрип, вибрация руля при торможении или «тянет» в сторону — повод проверить раньше.",
        ),
        Service(
            id = "battery",
            title = "Аккумулятор",
            pics = listOf(R.drawable.pic_battery),
            action = "Проверка",
            km = 20_000, months = 12,
            kmHard = 20_000, monthsHard = 12,
            what = "Проверить напряжение и клеммы, перед зимой — нагрузочный тест.",
            parts = listOf(PartNo("Если есть i-stop", "EFB типа Q-85, такой же, как стоит")),
            tips = "Обычный срок жизни 4–5 лет. С системой i-stop ставь только EFB/AGM нужного типа, иначе старт-стоп будет отключаться.",
        ),
        Service(
            id = "tires",
            title = "Колёса и шины",
            pics = listOf(R.drawable.pic_tires),
            action = "Перестановка",
            km = 10_000, months = 12,
            kmHard = 8_000, monthsHard = 6,
            what = "Переставить колёса перёд-зад, проверить давление и износ, сбросить датчик давления (TPMS), если есть.",
            tips = "Сезонная смена резины — хороший момент заодно переставить колёса по осям.",
            official = true,
        ),
        Service(
            id = "wipers",
            title = "Щётки стеклоочистителя",
            pics = listOf(R.drawable.pic_wipers),
            action = "Замена",
            km = null, months = 12,
            kmHard = null, monthsHard = 12,
            what = "Поменять резинки или щётки целиком.",
            tips = "Полосит или скрипит — пора. Зимние щётки удобнее менять осенью.",
            official = false,
        ),
        Service(
            id = "suspension",
            title = "Подвеска, рулевое и пыльники",
            pics = listOf(R.drawable.pic_suspension),
            action = "Проверка",
            km = 40_000, months = 24,
            kmHard = 20_000, monthsHard = 12,
            what = "Осмотреть шаровые, люфт ступичных подшипников, рулевые тяги, пыльники ШРУС, сайлентблоки.",
            tips = "Порванный пыльник ШРУСа — дешёвая замена сейчас вместо дорогого ШРУСа потом.",
        ),
        Service(
            id = "exhaust",
            title = "Выхлопная система",
            pics = listOf(R.drawable.pic_exhaust),
            action = "Проверка",
            km = 80_000, months = 60,
            kmHard = 80_000, monthsHard = 60,
            what = "Осмотреть трубы, глушитель, крепления и тепловые экраны.",
        ),
    )

    fun byId(id: String): Service? = all.firstOrNull { it.id == id }
}
