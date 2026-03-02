package com.example.sip_sip_mobile_app

enum class ActivityLevel {
    NONE, LIGHT, MODERATE, HEAVY
}

enum class Gender {
    MALE, FEMALE, OTHER
}

object WaterIntakeCalculator {
    fun calculateDailyWater(weightKg: Double, gender: Gender, activityLevel: ActivityLevel): Int {
        // สูตรมาตรฐาน: น้ำหนัก (กก.) * 33 (มล.)
        var totalWaterMl = weightKg * 33.0

        // บวกโบนัสตามระดับกิจกรรม
        val extraWater = when (activityLevel) {
            ActivityLevel.NONE -> 0.0
            ActivityLevel.LIGHT -> 300.0
            ActivityLevel.MODERATE -> 600.0
            ActivityLevel.HEAVY -> 1000.0
        }
        
        totalWaterMl += extraWater

        // ปรับตามเพศ (เพศชายมักต้องการน้ำมากกว่าเล็กน้อย)
        if (gender == Gender.MALE) {
            totalWaterMl += 250.0
        }

        // ปัดให้เป็นเลขกลมๆ หลักสิบ
        return (Math.round(totalWaterMl / 10.0) * 10).toInt()
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
}
