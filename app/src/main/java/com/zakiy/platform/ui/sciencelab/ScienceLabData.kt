package com.zakiy.platform.ui.sciencelab

import com.zakiy.platform.R

/** بيانات مستكشف الأحياء بمختبر العلوم - منسوخة حرفيًا من مصدر الحقيقة
 * بالموقع (website/src/js/30-science-lab.js: SL_BIO_CATEGORIES, SL_ANIMALS,
 * SL_BODY_IMAGES, SL_ANIMAL_BODY_KEY, SL_PARTS_INFO) - لا نضيف أو نغيّر أي
 * محتوى، بس ننقله لموديلات Kotlin + string resources (مطابقةً لقاعدة
 * "zero inline Arabic literal strings" المتبعة بباقي الشاشات). */

/** الأعضاء اللي تظهر كنقاط ضغط (hotspots) على صور الجسم - نفس الثمانية
 * بـ SL_PARTS_INFO بالضبط (SL_BODY_PARTS.animal_generic وSL_BODY_PARTS.human
 * بالموقع فيهم نفس القائمة الكاملة، فما نحتاج نفلتر شي - كل hotspot موجود
 * بصورة معينة نعرضه زي ما هو). */
enum class SlBodyPart(val id: String, val nameRes: Int, val descRes: Int) {
    Heart("heart", R.string.sl_part_heart_name, R.string.sl_part_heart_desc),
    Lungs("lungs", R.string.sl_part_lungs_name, R.string.sl_part_lungs_desc),
    Brain("brain", R.string.sl_part_brain_name, R.string.sl_part_brain_desc),
    Stomach("stomach", R.string.sl_part_stomach_name, R.string.sl_part_stomach_desc),
    Kidneys("kidneys", R.string.sl_part_kidneys_name, R.string.sl_part_kidneys_desc),
    Skin("skin", R.string.sl_part_skin_name, R.string.sl_part_skin_desc),
    Liver("liver", R.string.sl_part_liver_name, R.string.sl_part_liver_desc),
    Intestines("intestines", R.string.sl_part_intestines_name, R.string.sl_part_intestines_desc),
    ;

    companion object {
        fun fromId(id: String): SlBodyPart? = entries.firstOrNull { it.id == id }
    }
}

/** نقطة ضغط فوق صورة الجسم - x/y نسبة مئوية من عرض/ارتفاع الصورة (مطابقة
 * تمامًا لإحداثيات SL_BODY_IMAGES بالموقع - مُتحقّق منها بدقة على الصور
 * الحقيقية، ما نعيد حسابها). */
data class SlHotspot(val part: SlBodyPart, val x: Float, val y: Float)

/** صورة تشريح جسم/حيوان - imageModel إما رابط ويكيميديا خارجي (String) أو
 * drawable محلي (Int، للفيل والتمساح بس - رسومات المستخدم المرفقة بالمشروع). */
data class SlBodyImage(
    val key: String,
    val imageModel: Any,
    val creditRes: Int? = null,
    val hotspots: List<SlHotspot>,
)

data class SlAnimal(
    val id: String,
    val icon: String,
    val nameRes: Int,
    val quickFactRes: Int,
    val factRes: List<Int>,
    val bodyKey: String,
)

data class SlCategory(
    val id: String,
    val icon: String,
    val nameRes: Int,
    val animalIds: List<String>,
)

object ScienceLabData {

    // SL_BODY_IMAGES - كل الصور + نقاط الضغط عليها، حرفيًا من الموقع
    val bodyImages: Map<String, SlBodyImage> = listOf(
        SlBodyImage(
            key = "human",
            imageModel = "https://upload.wikimedia.org/wikipedia/commons/e/e3/Internal_organs.svg",
            hotspots = listOf(
                SlHotspot(SlBodyPart.Brain, 48.6f, 16.4f),
                SlHotspot(SlBodyPart.Heart, 44.3f, 52.4f),
                SlHotspot(SlBodyPart.Lungs, 35.7f, 44f),
                SlHotspot(SlBodyPart.Lungs, 50f, 44f),
                SlHotspot(SlBodyPart.Liver, 42.1f, 63.2f),
                SlHotspot(SlBodyPart.Stomach, 52.5f, 68.4f),
                SlHotspot(SlBodyPart.Kidneys, 38.9f, 72f),
                SlHotspot(SlBodyPart.Kidneys, 52.1f, 70.4f),
                SlHotspot(SlBodyPart.Intestines, 40f, 82.8f),
                SlHotspot(SlBodyPart.Skin, 29.3f, 48f),
            ),
        ),
        SlBodyImage(
            key = "dog",
            imageModel = "https://upload.wikimedia.org/wikipedia/commons/c/c5/Dog_Internal_Anatomy.svg",
            creditRes = R.string.sl_body_credit_dog,
            hotspots = listOf(
                SlHotspot(SlBodyPart.Brain, 16.7f, 9.2f),
                SlHotspot(SlBodyPart.Lungs, 17.8f, 26.7f),
                SlHotspot(SlBodyPart.Heart, 20.6f, 35f),
                SlHotspot(SlBodyPart.Liver, 29.4f, 28.3f),
                SlHotspot(SlBodyPart.Stomach, 31.1f, 34.2f),
                SlHotspot(SlBodyPart.Kidneys, 36.1f, 25f),
                SlHotspot(SlBodyPart.Intestines, 39.4f, 31.7f),
                SlHotspot(SlBodyPart.Skin, 33.3f, 46.7f),
            ),
        ),
        SlBodyImage(
            key = "elephant",
            imageModel = R.drawable.sl_body_elephant,
            hotspots = listOf(
                SlHotspot(SlBodyPart.Brain, 22.5f, 30.2f),
                SlHotspot(SlBodyPart.Heart, 40.7f, 40.9f),
                SlHotspot(SlBodyPart.Liver, 48.7f, 42.7f),
                SlHotspot(SlBodyPart.Stomach, 52.2f, 47.4f),
                SlHotspot(SlBodyPart.Intestines, 62.5f, 48.6f),
                SlHotspot(SlBodyPart.Kidneys, 72f, 26.7f),
                SlHotspot(SlBodyPart.Kidneys, 75.2f, 34.4f),
            ),
        ),
        SlBodyImage(
            key = "crocodile",
            imageModel = R.drawable.sl_body_crocodile,
            hotspots = listOf(
                SlHotspot(SlBodyPart.Brain, 23f, 19.4f),
                SlHotspot(SlBodyPart.Heart, 42.8f, 47.5f),
                SlHotspot(SlBodyPart.Liver, 44.9f, 41f),
                SlHotspot(SlBodyPart.Lungs, 52.2f, 36.7f),
                SlHotspot(SlBodyPart.Stomach, 52.8f, 48.6f),
                SlHotspot(SlBodyPart.Intestines, 64.8f, 41f),
                SlHotspot(SlBodyPart.Kidneys, 72.6f, 37.8f),
                SlHotspot(SlBodyPart.Skin, 86.7f, 64.8f),
            ),
        ),
        SlBodyImage(
            key = "cat",
            imageModel = "https://upload.wikimedia.org/wikipedia/commons/5/5a/Scheme_cat_anatomy.svg",
            creditRes = R.string.sl_body_credit_cat,
            hotspots = listOf(
                SlHotspot(SlBodyPart.Brain, 27.9f, 25.5f),
                SlHotspot(SlBodyPart.Lungs, 43.3f, 40.5f),
                SlHotspot(SlBodyPart.Heart, 53.9f, 45.1f),
                SlHotspot(SlBodyPart.Liver, 59.1f, 42.8f),
                SlHotspot(SlBodyPart.Stomach, 63.3f, 40.5f),
                SlHotspot(SlBodyPart.Kidneys, 65.1f, 34.7f),
                SlHotspot(SlBodyPart.Intestines, 67.3f, 44f),
                SlHotspot(SlBodyPart.Skin, 45.2f, 76.4f),
            ),
        ),
        SlBodyImage(
            key = "reptile",
            imageModel = "https://upload.wikimedia.org/wikipedia/commons/4/4d/Snake-anatomy.svg",
            creditRes = R.string.sl_body_credit_reptile,
            hotspots = listOf(
                SlHotspot(SlBodyPart.Heart, 50f, 15f),
                SlHotspot(SlBodyPart.Lungs, 41.7f, 15f),
                SlHotspot(SlBodyPart.Liver, 25f, 50f),
                SlHotspot(SlBodyPart.Stomach, 50f, 35.8f),
                SlHotspot(SlBodyPart.Intestines, 82.8f, 33.7f),
                SlHotspot(SlBodyPart.Kidneys, 75f, 81.7f),
                SlHotspot(SlBodyPart.Skin, 10f, 78.3f),
            ),
        ),
        SlBodyImage(
            key = "fish",
            imageModel = "https://upload.wikimedia.org/wikipedia/commons/b/b0/Fish-anatomy.svg",
            creditRes = R.string.sl_body_credit_fish,
            hotspots = listOf(
                SlHotspot(SlBodyPart.Heart, 28.9f, 58.8f),
                SlHotspot(SlBodyPart.Liver, 40f, 55.5f),
                SlHotspot(SlBodyPart.Stomach, 44.3f, 59.1f),
                SlHotspot(SlBodyPart.Intestines, 38.5f, 68.4f),
                SlHotspot(SlBodyPart.Kidneys, 47.2f, 48.1f),
                SlHotspot(SlBodyPart.Skin, 67.4f, 27.7f),
            ),
        ),
        SlBodyImage(
            key = "whale",
            imageModel = "https://upload.wikimedia.org/wikipedia/commons/8/88/Orca_internal_anatomy.svg",
            creditRes = R.string.sl_body_credit_whale,
            hotspots = listOf(
                SlHotspot(SlBodyPart.Brain, 14f, 51.7f),
                SlHotspot(SlBodyPart.Heart, 29.6f, 68.1f),
                SlHotspot(SlBodyPart.Lungs, 28.6f, 46.6f),
                SlHotspot(SlBodyPart.Liver, 40.1f, 64.7f),
                SlHotspot(SlBodyPart.Stomach, 41.3f, 54.8f),
                SlHotspot(SlBodyPart.Kidneys, 50.7f, 56f),
                SlHotspot(SlBodyPart.Intestines, 47.5f, 65.5f),
                SlHotspot(SlBodyPart.Skin, 67.9f, 30.2f),
            ),
        ),
        SlBodyImage(
            key = "turtle",
            imageModel = "https://upload.wikimedia.org/wikipedia/commons/c/cb/Scheme_turtle_anatomy-numbers.svg",
            creditRes = R.string.sl_body_credit_turtle,
            hotspots = listOf(
                SlHotspot(SlBodyPart.Lungs, 74f, 37.9f),
                SlHotspot(SlBodyPart.Heart, 74.8f, 40.4f),
                SlHotspot(SlBodyPart.Stomach, 71.8f, 46.5f),
                SlHotspot(SlBodyPart.Liver, 78.5f, 45.6f),
                SlHotspot(SlBodyPart.Intestines, 74f, 55.1f),
                SlHotspot(SlBodyPart.Skin, 65.1f, 37.9f),
            ),
        ),
        SlBodyImage(
            key = "frog",
            imageModel = "https://upload.wikimedia.org/wikipedia/commons/0/0e/The_biology_of_the_frog_%28Page_75%2C_Fig._10%29_BHL7720765.jpg",
            creditRes = R.string.sl_body_credit_frog,
            hotspots = listOf(
                SlHotspot(SlBodyPart.Heart, 44.9f, 6.7f),
                SlHotspot(SlBodyPart.Lungs, 30.7f, 8.5f),
                SlHotspot(SlBodyPart.Lungs, 63.8f, 10.7f),
                SlHotspot(SlBodyPart.Liver, 54.4f, 8.2f),
                SlHotspot(SlBodyPart.Kidneys, 28.4f, 18.3f),
            ),
        ),
        SlBodyImage(
            key = "bird",
            imageModel = "https://upload.wikimedia.org/wikipedia/commons/1/1c/Gastrointestinal_track_of_the_Mallard-Uklad_pokarmowy_krzyzowki.svg",
            creditRes = R.string.sl_body_credit_bird,
            hotspots = listOf(
                SlHotspot(SlBodyPart.Stomach, 48.5f, 75f),
                SlHotspot(SlBodyPart.Intestines, 37.5f, 65.4f),
            ),
        ),
    ).associateBy { it.key }

    // SL_ANIMALS + SL_ANIMAL_BODY_KEY مدموجين - كل حيوان مع مفتاح صورة جسمه
    val animals: Map<String, SlAnimal> = listOf(
        SlAnimal("lion", "🦁", R.string.sl_animal_lion_name, R.string.sl_animal_lion_quick,
            listOf(R.string.sl_animal_lion_fact1, R.string.sl_animal_lion_fact2, R.string.sl_animal_lion_fact3), "cat"),
        SlAnimal("elephant", "🐘", R.string.sl_animal_elephant_name, R.string.sl_animal_elephant_quick,
            listOf(R.string.sl_animal_elephant_fact1, R.string.sl_animal_elephant_fact2, R.string.sl_animal_elephant_fact3), "elephant"),
        SlAnimal("cat", "🐱", R.string.sl_animal_cat_name, R.string.sl_animal_cat_quick,
            listOf(R.string.sl_animal_cat_fact1, R.string.sl_animal_cat_fact2, R.string.sl_animal_cat_fact3), "cat"),
        // كلب - أُضيف لاحقًا لتصنيف الثدييات (نفس صورة/نقاط SL_BODY_IMAGES.dog
        // الموجودة أصلًا)، ترتيبه بين القط والحوت زي الموقع بالضبط
        SlAnimal("dog", "🐶", R.string.sl_animal_dog_name, R.string.sl_animal_dog_quick,
            listOf(R.string.sl_animal_dog_fact1, R.string.sl_animal_dog_fact2, R.string.sl_animal_dog_fact3), "dog"),
        SlAnimal("whale", "🐋", R.string.sl_animal_whale_name, R.string.sl_animal_whale_quick,
            listOf(R.string.sl_animal_whale_fact1, R.string.sl_animal_whale_fact2, R.string.sl_animal_whale_fact3), "whale"),
        SlAnimal("crocodile", "🐊", R.string.sl_animal_crocodile_name, R.string.sl_animal_crocodile_quick,
            listOf(R.string.sl_animal_crocodile_fact1, R.string.sl_animal_crocodile_fact2, R.string.sl_animal_crocodile_fact3), "crocodile"),
        SlAnimal("turtle", "🐢", R.string.sl_animal_turtle_name, R.string.sl_animal_turtle_quick,
            listOf(R.string.sl_animal_turtle_fact1, R.string.sl_animal_turtle_fact2, R.string.sl_animal_turtle_fact3), "turtle"),
        SlAnimal("snake", "🐍", R.string.sl_animal_snake_name, R.string.sl_animal_snake_quick,
            listOf(R.string.sl_animal_snake_fact1, R.string.sl_animal_snake_fact2, R.string.sl_animal_snake_fact3), "reptile"),
        SlAnimal("frog", "🐸", R.string.sl_animal_frog_name, R.string.sl_animal_frog_quick,
            listOf(R.string.sl_animal_frog_fact1, R.string.sl_animal_frog_fact2, R.string.sl_animal_frog_fact3), "frog"),
        SlAnimal("salamander", "🦎", R.string.sl_animal_salamander_name, R.string.sl_animal_salamander_quick,
            listOf(R.string.sl_animal_salamander_fact1, R.string.sl_animal_salamander_fact2, R.string.sl_animal_salamander_fact3), "frog"),
        SlAnimal("eagle", "🦅", R.string.sl_animal_eagle_name, R.string.sl_animal_eagle_quick,
            listOf(R.string.sl_animal_eagle_fact1, R.string.sl_animal_eagle_fact2, R.string.sl_animal_eagle_fact3), "bird"),
        SlAnimal("penguin", "🐧", R.string.sl_animal_penguin_name, R.string.sl_animal_penguin_quick,
            listOf(R.string.sl_animal_penguin_fact1, R.string.sl_animal_penguin_fact2, R.string.sl_animal_penguin_fact3), "bird"),
        SlAnimal("parrot", "🦜", R.string.sl_animal_parrot_name, R.string.sl_animal_parrot_quick,
            listOf(R.string.sl_animal_parrot_fact1, R.string.sl_animal_parrot_fact2, R.string.sl_animal_parrot_fact3), "bird"),
        SlAnimal("shark", "🦈", R.string.sl_animal_shark_name, R.string.sl_animal_shark_quick,
            listOf(R.string.sl_animal_shark_fact1, R.string.sl_animal_shark_fact2, R.string.sl_animal_shark_fact3), "fish"),
        SlAnimal("goldfish", "🐠", R.string.sl_animal_goldfish_name, R.string.sl_animal_goldfish_quick,
            listOf(R.string.sl_animal_goldfish_fact1, R.string.sl_animal_goldfish_fact2, R.string.sl_animal_goldfish_fact3), "fish"),
    ).associateBy { it.id }

    // SL_BIO_CATEGORIES - ترتيب الثدييات: أسد، فيل، قط، كلب، حوت (نفس ترتيب الموقع)
    val categories: List<SlCategory> = listOf(
        SlCategory("mammals", "🦁", R.string.sl_cat_mammals, listOf("lion", "elephant", "cat", "dog", "whale")),
        SlCategory("reptiles", "🐊", R.string.sl_cat_reptiles, listOf("crocodile", "turtle", "snake")),
        SlCategory("amphibians", "🐸", R.string.sl_cat_amphibians, listOf("frog", "salamander")),
        SlCategory("birds", "🦅", R.string.sl_cat_birds, listOf("eagle", "penguin", "parrot")),
        SlCategory("fish", "🦈", R.string.sl_cat_fish, listOf("shark", "goldfish")),
        SlCategory("human", "🧍", R.string.sl_cat_human, emptyList()),
    )
}
