package com.example.sip_sip_mobile_app

enum class ActivityLevel {
    NONE, LIGHT, MODERATE, HEAVY
}

enum class Gender {
    MALE, FEMALE, OTHER
}

fun calculateDailyWater(weightKg: Double, gender: Gender, activityLevel: ActivityLevel): Int {
    var totalWaterMl = weightKg * 33.0

    val extraWater = when (activityLevel) {
        ActivityLevel.NONE -> 0.0
        ActivityLevel.LIGHT -> 300.0
        ActivityLevel.MODERATE -> 600.0
        ActivityLevel.HEAVY -> 950.0
    }
    
    totalWaterMl += extraWater

    if (gender == Gender.FEMALE) {
        totalWaterMl *= 0.95 
    }

    return totalWaterMl.toInt()
}

fun mapGender(genderString: String): Gender {
    return when (genderString) {
        "ชาย" -> Gender.MALE
        "หญิง" -> Gender.FEMALE
        else -> Gender.OTHER
    }
}

fun mapActivityLevel(activityString: String): ActivityLevel {
    return when (activityString) {
        "ไม่ออกกำลังกาย" -> ActivityLevel.NONE
        "เล็กน้อย" -> ActivityLevel.LIGHT
        "ปานกลาง" -> ActivityLevel.MODERATE
        "หนัก" -> ActivityLevel.HEAVY
        else -> ActivityLevel.NONE
    }
}
